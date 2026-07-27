---
name: review
description: Code review checklist for this ecommerce project. Checks dead errors, orphans, message bundles, transactions, order/payment invariants, silent no-ops, and security. Run before opening a PR.
user-invocable: true
allowed-tools:
  - Read
  - Bash
---

# /review — PR Review Checklist

Run each check against the current branch. Report by section: file, line (if applicable), what's wrong, and the fix.

---

## 1. Dead error types

For every constant in `ECOMErrorType.java`, grep `src/` (exclude the enum file). Flag zero references.

**Fix:** Delete the constant and its keys from `messages.properties` + `messages_fa.properties`.

---

## 2. Orphaned entities / repositories

For every class under `persistence/entity/` and `persistence/repository/`, grep `src/` (exclude self). Flag zero external references.

If the entity is gone but tables remain in Flyway history, add `V1.x__drop_*.sql` (`DROP … IF EXISTS` for tables, FKs, sequences). Also drop bare `Long` FK columns on other entities.

---

## 3. Message bundle drift

Every `ECOMErrorType.messageKey` ↔ both `messages.properties` and `messages_fa.properties` (no stale keys, no missing translations).

---

## 4. `@Transactional` on services

Under `application/service/`, flag:
- Class-level `@Transactional` without `readOnly=true`
- Reads with no `@Transactional` (LazyInitialization risk on MapStruct/collections)
- Reads with plain `@Transactional` instead of `readOnly=true`

Expected: method-level only — writes `@Transactional`, reads `@Transactional(readOnly=true)`.

---

## 5. Order / payment invariants

When the PR touches orders, checkout, payment, or `order_transaction`:

| Rule | Expectation |
|------|-------------|
| Status writes | Load via `findByIdForUpdate` / `findByIdAndUserIdForUpdate` (same lock family as `findExpiredReservations`) |
| Cancel | Only from `RESERVED`/`PAID`; restore inventory; no auto `REFUND`. Paid cancel leaves PAYMENT only until admin records refund |
| Refund | Admin: list refundable (cancelled + PAYMENT + no REFUND); POST refund with reference+iban → `REFUND` tx |
| Pay | JWT required (`/pay`). Guest checkout returns no token — signup/login same mobile, then pay. Confirm stays public |
| Expiry | `ReservationReleaseService` + `pg_try_advisory_xact_lock` (other instances skip). Job gated by `app.checkout.scheduling.enabled` |
| Stock | Decrement at checkout; restore on cancel/`FAILED` — never double-restore |

Flag missing locks, pay/refund without a `Transaction` row, or refund on non-refundable order.

---

## 6. Silent no-ops on conditional params

Flag `@RequestParam(required=false)` that is required for another param's value (e.g. `imageId` when `type=OTHER`) if null yields 2xx and no work.

**Fix:** Guard → `VALIDATION_ERROR`; add ITest asserting 400.

---

## 7. Security

Read `SecurityConfiguration` + `PublicEndPoint`.

- Controllers: `@RequestMapping` must start with `/api`
- Matcher order: POST public → GET public → `/api/**` authenticated → static → SPA no-dot wildcards → `anyRequest` authenticated. `/api/**` **before** SPA wildcards (else unprotected API GETs)
- No swagger paths unless `springdoc` is in `pom.xml`
- CORS disabled (same-origin SPA)
- `SpaController` (`GET /`) must exist (WelcomePageHandlerMapping 500 without it)
