#!/bin/bash
# Recreate the app container from IMAGE, tagged locally as rivani:latest.
# Expected env: IMAGE (ghcr.io/owner/repo:sha)
# Optional: GHCR_USER + GHCR_TOKEN for private GHCR pulls.
set -euo pipefail

: "${IMAGE:?IMAGE is required}"
DEPLOY_DIR="${DEPLOY_DIR:-/opt/rivani}"

if [ -n "${GHCR_TOKEN:-}" ]; then
  : "${GHCR_USER:?GHCR_USER is required when GHCR_TOKEN is set}"
  echo "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USER" --password-stdin
fi

if [ "${SKIP_PULL:-}" != "1" ]; then
  docker pull "$IMAGE"
fi
docker tag "$IMAGE" rivani:latest

if [ -n "${GHCR_TOKEN:-}" ]; then
  docker logout ghcr.io >/dev/null
fi

cd "$DEPLOY_DIR"
docker compose --env-file .env up -d --remove-orphans
docker compose --env-file .env up -d --force-recreate --no-deps ecom

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
