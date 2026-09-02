---
name: deploy-tar
description: Manually deploy a saved Docker image tar (`docker save` output) to the VPS when the automated CD/GHCR pull path is down. Uploads the tar, loads it, re-tags to rivani:latest, recreates only the ecom container, and verifies health. Use when CD fails to reach ghcr.io.
user-invocable: true
allowed-tools:
  - Read
  - Bash
---

# /deploy-tar — Manual image-tar deploy to the VPS

Fallback for [deploy.md](../../rules/deploy.md) when the CD pipeline can't push/pull through
`ghcr.io` (the VPS `watch-image.timer` logs `TLS handshake timeout`). Ships a local
`docker save` tar straight to the server over SSH — no registry involved.

The stack: compose service `ecom` runs image **`rivani:latest`** in `/opt/rivani`; Host Nginx
proxies `:80 → 127.0.0.1:8080`. Postgres/Redis + their data volumes are **not** touched by this flow.

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
print('RepoTags:', m.get('RepoTags'))          # often ['rivani-v1:latest'] — NOT rivani:latest
print('arch/os :', c.get('architecture'), c.get('os'))   # must be: amd64 linux
print('created :', c.get('created'))
PY
```

Abort if arch ≠ `amd64` — rebuild with `docker build --platform linux/amd64 …` first.
Note the `RepoTags` value; call it `LOADED_TAG` below (the compose stack wants `rivani:latest`,
but `docker save` usually preserves the build tag like `rivani-v1:latest`).

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

`update-app.sh` already does tag → recreate `ecom` → wait-for-health → prune, so feed it the loaded
tag with `SKIP_PULL=1` (image is already local — no registry pull):

```bash
$SSH $SERVER 'IMAGE=<LOADED_TAG> SKIP_PULL=1 DEPLOY_DIR=/opt/rivani bash /opt/rivani/update-app.sh'
```

It re-tags `<LOADED_TAG> → rivani:latest`, force-recreates **only** `ecom` (`--no-deps`), waits up to
180 s for health, then `docker image prune -f`. Success prints `app is healthy (<LOADED_TAG>)`.

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
- **watch-image.timer race.** The per-minute timer keeps trying `ghcr.io/hardasan/ecom:main`. While
  that pull fails it's harmless (exits before touching the container). But if GHCR recovers and an
  **older** `:main` image exists, the timer can redeploy over your manual image. To hold your build
  in place: `systemctl stop rivani-update.timer` before the deploy and start it again once CD is fixed.
- **Permanent fix** is the VPS → `ghcr.io` connectivity itself (DNS / MTU / firewall behind the
  `TLS handshake timeout`), not this flow.
