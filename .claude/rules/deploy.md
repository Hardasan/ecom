# Deploy Rules

Non-obvious CI/CD only. Architecture → [CLAUDE.md](../../CLAUDE.md).

---

## Pipelines

| Phase | File | When | What |
|-------|------|------|------|
| **CI** | `.github/workflows/ci.yml` | PR to `main`, or push to `main` | Tests only. No image, no deploy. `skip cd` does **not** skip CI. |
| **CD** | `.github/workflows/cd.yml` | Push/merge to `main`, or **Actions → CD → Run workflow** | Publish image (`:sha`, `:main`) then deploy. Does **not** re-run tests. |
| **Release** | `.github/workflows/release.yml` | Push tag `v*` | Tests + GitHub Release jar + versioned image (`0.1.0`, `latest`) then deploy that image. |
| *(helper)* | `.github/workflows/deploy-server.yml` | Called by CD and Release | `docker pull` + recreate `ecom`. Not a phase. |

Default branch is **`main`**.

---

## Skip CD

A push or merge to `main` deploys unless the **head commit message** contains `skip cd` (or `[skip cd]`):

```bash
git commit -m "tweak copy skip cd"
git push origin main
```

On a GitHub PR merge, put `skip cd` in the merge/squash commit message.

Deploy later: **Actions → CD → Run workflow**, or:

```bash
git tag v0.1.0 && git push origin v0.1.0
```

`git tag` alone is local; GitHub only sees the tag after `git push origin v0.1.0`.

---

## Image and host

- Registry: `ghcr.io/<owner>/ecom` (lowercase). CD tags `:main` and `:<sha>`. Release tags semver + `latest`.
- VPS: `root@45.94.215.219`, stack `/opt/rivani`, compose service `ecom`, local image `rivani:latest`.
- Host Nginx proxies to `127.0.0.1:8080` (`deploy/nginx-rivani.conf`).
- A systemd timer on the VPS also pulls `:main` if the digest changed (`deploy/watch-image.sh`).

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
