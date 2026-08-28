# Deploy Rules

Non-obvious CI/CD only. Architecture → [CLAUDE.md](../../CLAUDE.md).

---

## Pipelines

| Phase | File | When | What |
|-------|------|------|------|
| **CI** | `.github/workflows/ci.yml` | PR to `main`, or push to `main` | Tests + Docker *build check*. No publish, no deploy. `skip cd` does **not** skip CI. |
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
