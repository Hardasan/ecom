#!/bin/bash
# Roll out a new app image via docker compose: pull it from GHCR, then recreate ecom.
# This is exactly the manual flow (`docker compose pull ecom && docker compose up -d`),
# just driven over SSH from CI.
#
# Expected env: IMAGE (ghcr.io/owner/repo:TAG) — TAG selects which published image to run.
# Optional: GHCR_USER + GHCR_TOKEN for private GHCR pulls.
#           SKIP_PULL=1 to skip the registry pull (image is already present locally,
#           e.g. the deploy-tar fallback when ghcr.io is unreachable).
set -euo pipefail

: "${IMAGE:?IMAGE is required}"
DEPLOY_DIR="${DEPLOY_DIR:-/opt/rivani}"
# The compose file reads ${ECOM_TAG:-latest}; run the tag this workflow published.
export ECOM_TAG="${IMAGE##*:}"

if [ -n "${GHCR_TOKEN:-}" ]; then
  : "${GHCR_USER:?GHCR_USER is required when GHCR_TOKEN is set}"
  echo "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USER" --password-stdin
fi

cd "$DEPLOY_DIR"
if [ "${SKIP_PULL:-}" != "1" ]; then
  docker compose --env-file .env pull ecom
fi
docker compose --env-file .env up -d --remove-orphans
docker compose --env-file .env up -d --force-recreate --no-deps ecom

if [ -n "${GHCR_TOKEN:-}" ]; then
  docker logout ghcr.io >/dev/null
fi

echo "waiting for health..."
status=""
for _ in $(seq 1 36); do
  status="$(docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' rivani)"
  if [ "$status" = "healthy" ]; then
    echo "app is healthy ($IMAGE)"
    docker image prune -f >/dev/null
    exit 0
  fi
  if [ "$status" = "exited" ] || [ "$status" = "dead" ]; then
    echo "container $status"
    docker logs --tail 100 rivani
    exit 1
  fi
  sleep 5
done

echo "timed out waiting for health (last status: ${status:-unknown})"
docker logs --tail 100 rivani
exit 1
