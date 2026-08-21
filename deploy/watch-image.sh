#!/bin/bash
# Poll GHCR for a newer :main image and recreate the app only when the digest changes.
set -euo pipefail

DEPLOY_DIR="${DEPLOY_DIR:-/opt/rivani}"
# shellcheck disable=SC1091
if [ -f "$DEPLOY_DIR/image.env" ]; then
  set -a
  # shellcheck disable=SC1091
  . "$DEPLOY_DIR/image.env"
  set +a
fi
IMAGE="${IMAGE:-ghcr.io/hardasan/ecom:main}"

before="$(docker image inspect "$IMAGE" --format '{{index .RepoDigests 0}}' 2>/dev/null || true)"

if [ -f "$DEPLOY_DIR/ghcr.token" ]; then
  GHCR_USER="${GHCR_USER:-$(tr -d '[:space:]' < "$DEPLOY_DIR/ghcr.user" 2>/dev/null || echo x-access-token)}"
  GHCR_TOKEN="$(tr -d '[:space:]' < "$DEPLOY_DIR/ghcr.token")"
  echo "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USER" --password-stdin >/dev/null
fi

if ! docker pull "$IMAGE"; then
  echo "pull failed for $IMAGE (image may not exist yet)"
  docker logout ghcr.io >/dev/null 2>&1 || true
  exit 0
fi

docker logout ghcr.io >/dev/null 2>&1 || true

after="$(docker image inspect "$IMAGE" --format '{{index .RepoDigests 0}}')"
if [ -n "$before" ] && [ "$before" = "$after" ]; then
  echo "no change ($IMAGE)"
  exit 0
fi

echo "new image $after — deploying"
IMAGE="$IMAGE" DEPLOY_DIR="$DEPLOY_DIR" SKIP_PULL=1 bash "$DEPLOY_DIR/update-app.sh"
