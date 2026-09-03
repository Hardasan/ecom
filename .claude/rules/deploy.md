# Deploy Rules

Non-obvious CI/CD only. Architecture → [CLAUDE.md](../../CLAUDE.md).

---

## Pipelines

| Phase | File | When | What |
|-------|------|------|------|
| **CI** | `.github/workflows/ci.yml` | PR to `main`, or push to `main` | Tests only. No image, no deploy. |
| **Release** | `.github/workflows/release.yml` | Push tag `v*` | Tests + GitHub Release jar + versioned GHCR image (`0.1.0`, `0.1`, `latest`), then SSH-deploy that image to the VPS. |
| *(helper)* | `.github/workflows/deploy-server.yml` | Called by Release | SSH → `docker compose pull ecom` + `up -d`. Not a phase. |

Default branch is **`main`**. **Deploys happen only on a version tag** — pushes to `main` just run CI.

---

## Cut a release (the only deploy path)

```bash
git tag v0.1.0 && git push origin v0.1.0
```

`git tag` alone is local; GitHub only sees the tag — and runs Release — after `git push origin v0.1.0`.
Release then: builds + tests → publishes the image to GHCR (`0.1.0`, `0.1`, `latest`) → SSHes to the
VPS → rolls it out with `docker compose pull ecom && docker compose up -d`. Nothing else auto-deploys.

---

## Image and host

- Registry: `ghcr.io/<owner>/ecom` (lowercase). Release tags semver (`0.1.0`, `0.1`) + `latest`.
- VPS: `root@45.94.215.219`, stack `/opt/rivani`, compose service `ecom`, image `ghcr.io/hardasan/ecom:${ECOM_TAG:-latest}` pulled straight from GHCR (`docker compose pull ecom && docker compose up -d`); set `ECOM_TAG` in `/opt/rivani/.env` to pin/roll back.
- Host Nginx proxies to `127.0.0.1:8080` (`deploy/nginx-rivani.conf`).

---

## Admin dashboard subdomain

The staff dashboard is served on **`dashboard.rivany.ir`**, the storefront on `rivany.ir` — one
host-aware Angular build, one backend. The browser hostname selects the mode (`core/host.ts`):
`dashboard.*` → dashboard route table (`admin.routes.ts`: shared login + `/admin` + `/warehouse`);
anything else → storefront. Staff sign in with **mobile + password** (`POST /user/login`); the shared
login then routes by role — `ROLE_ADMIN` → `/admin`, `ROLE_WAREHOUSE` → `/warehouse` (fulfillment
console), any other role is rejected. The shop's `/admin` path is gone.

One-time VPS setup (no image rebuild needed — it's front-end + Nginx only, already in the image):
1. **DNS**: `dashboard.rivany.ir` A record → `45.94.215.219`.
2. **TLS**: `certbot --nginx -d dashboard.rivany.ir` (or add `-d dashboard.rivany.ir --expand`).
3. **Nginx**: install `deploy/nginx-dashboard-rivani.conf`, then `nginx -t && systemctl reload nginx`.

The existing `server_name _` catch-all already forwards the subdomain over HTTP, so step 3 is only for
the subdomain's own HTTPS vhost.
