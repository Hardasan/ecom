---
name: deploy-tar
description: Manually deploy a saved Docker image tar (`docker save` output) to the VPS when a release deploy can't pull the image from GHCR. Uploads the tar, loads it, re-tags to the GHCR ref the compose stack runs (ghcr.io/hardasan/ecom:latest), recreates only the ecom container, and verifies health. Use when the VPS can't reach ghcr.io.
user-invocable: true
allowed-tools:
  - Read
  - Bash
---

# /deploy-tar — Manual image-tar deploy to the VPS

Fallback for [deploy.md](../../rules/deploy.md) when a release deploy can't roll out because the VPS
can't reach `ghcr.io` (e.g. `docker compose pull` fails with `TLS handshake timeout`). Ships a local
`docker save` tar straight to the server over SSH — no registry involved.

The stack: compose service `ecom` runs image **`ghcr.io/hardasan/ecom:${ECOM_TAG:-latest}`** in
`/opt/rivani`; Host Nginx proxies `:80 → 127.0.0.1:8080`. Postgres/Redis + their data volumes are
**not** touched by this flow. This fallback loads the tar and re-tags it to that ref locally, so
`docker compose up` runs it without any registry pull.

---

## 0. Inputs — set once, run from the repo root

```bash
TAR=/path/to/rivani.tar          # the `docker save` output to deploy (ask if unknown)
KEY=~/.ssh/hardasan              # SSH key (see `ssh rivani.txt`; pass -i)
KH=deploy/known_hosts            # pinned host key — run from repo root, or use an absolute path
SERVER=root@45.94.215.219
SSH="ssh -i $KEY -o UserKnownHostsFile=$KH -o StrictHostKeyChecking=yes -o BatchMode=yes -o ConnectTimeout=20"
```

`StrictHostKeyChecking=yes` + the repo's pinned `known_hosts` is deliberate — it verifies the
server identity. Don't replace it with `accept-new`/`no`. `BatchMode=yes` fails fast instead of
hanging on a prompt (the shipped key is unencrypted, so no passphrase is expected).

---

## 1. Pre-flight — check arch BEFORE uploading (~257 MB)

The VPS is **amd64/linux**. A tar built on Apple Silicon without `--platform linux/amd64` is arm64
and will not run there. Read the tag + arch straight out of the tar:

```bash
python3 - "$TAR" <<'PY'
import sys, tarfile, json
t = tarfile.open(sys.argv[1])
m = json.load(t.extractfile('manifest.json'))[0]
c = json.load(t.extractfile(m['Config']))
print('RepoTags:', m.get('RepoTags'))          # often ['rivani-v1:latest'] — NOT the compose ref
print('arch/os :', c.get('architecture'), c.get('os'))   # must be: amd64 linux
print('created :', c.get('created'))
PY
```

Abort if arch ≠ `amd64` — rebuild with `docker build --platform linux/amd64 …` first.
Note the `RepoTags` value; call it `LOADED_TAG` below (the compose stack runs
`ghcr.io/hardasan/ecom:latest`, but `docker save` usually preserves the build tag like
`rivani-v1:latest`, so step 5 re-tags it).

---

## 2. Reachable + healthy? (read-only)

```bash
$SSH $SERVER 'docker ps --format "{{.Names}}\t{{.Image}}\t{{.Status}}"; df -h / | tail -1'
```

Confirm SSH works, the stack is up, and there's room for the tar + loaded image (need ~1 GB free).
Never print `/opt/rivani/.env` values (`POSTGRES_PASSWORD`, `JWT_SECRET_KEY`).

---

## 3. Upload + verify checksum

```bash
scp -i $KEY -o UserKnownHostsFile=$KH -o StrictHostKeyChecking=yes "$TAR" $SERVER:/root/rivani.tar
shasum -a 256 "$TAR"                         # local
$SSH $SERVER 'sha256sum /root/rivani.tar'    # remote — the two hashes MUST match
```

Large upload — run it in the background and poll `stat -c%s /root/rivani.tar` if it's slow. Do not
proceed to load until the checksums are identical.

---

## 4. Load

```bash
$SSH $SERVER 'docker load -i /root/rivani.tar'
```

Note the `Loaded image: <tag>` line — that's `LOADED_TAG` (e.g. `rivani-v1:latest`).

---

## 5. Tag + recreate — reuse the project's own script

The compose stack runs `ghcr.io/hardasan/ecom:${ECOM_TAG:-latest}`, so re-tag the loaded image to
that ref, then hand it to `update-app.sh` with `SKIP_PULL=1` (image is already local — no registry
pull). `update-app.sh` does recreate `ecom` → wait-for-health → prune:

```bash
$SSH $SERVER 'docker tag <LOADED_TAG> ghcr.io/hardasan/ecom:latest && \
  IMAGE=ghcr.io/hardasan/ecom:latest SKIP_PULL=1 DEPLOY_DIR=/opt/rivani bash /opt/rivani/update-app.sh'
```

With `SKIP_PULL=1` it skips the `docker compose pull`, force-recreates **only** `ecom` (`--no-deps`)
from the local image, waits up to 180 s for health, then `docker image prune -f`. Success prints
`app is healthy (ghcr.io/hardasan/ecom:latest)`.

---

## 6. Verify

```bash
$SSH $SERVER 'docker inspect rivani --format "ref={{.Config.Image}} id={{.Image}}"; \
  curl -s -m10 http://127.0.0.1:8080/actuator/health; echo; \
  curl -s -m10 -o /dev/null -w "nginx HTTP %{http_code}\n" http://127.0.0.1/actuator/health'
```

Expect: running `id=` equals the id `docker load` produced, `{"status":"UP"}`, and `nginx HTTP 200`.
(If the loaded id already equals what was running, it was a no-op redeploy — same build.)

---

## 7. Cleanup

```bash
$SSH $SERVER 'rm -f /root/rivani.tar'
```

---

## Notes

- **Data is safe.** Only `ecom` is recreated; `postgres_data` / `redis_data` volumes persist.
- **No auto-redeploy.** Deploys happen only on a version-tag Release, so nothing on the VPS overwrites
  your manually-loaded image on its own — it stays until the next release (or another manual deploy).
- **Permanent fix** is the VPS → `ghcr.io` connectivity itself (DNS / MTU / firewall behind the
  `TLS handshake timeout`), not this flow.
