# Business Rules

Non-obvious domain decisions only. Architecture → [CLAUDE.md](../../CLAUDE.md).

---

## User

- Mobile lives in both `username` (Security) and `mobile`.
- Login requires `isEnabled` **and** `isRegistered`.
- No single `name` column — use `firstName` + `lastName`.
- `iban` (`IR` + 24 digits): `GET/PUT /user/iban`. Optional until cancelling a **PAID** order (refund target).

---

## Auth

```
POST /user/signup-ticket → validation → signupToken
POST /user/signup                  ← signupToken + name + password
POST /user/check-registration
POST /user/login                   ← password → JWT
POST /user/login-ticket → validation → JWT
POST /user/change-password         ← new + confirm only (no current-password check)
```

`signupToken` in cache `SIGNUP_TOKEN` proves OTP done.

---

## OTP

Signup and login are fully isolated (caches, counters, blocks).

| | Config `app.{flow}.ticket.*` | Default |
|--|--|--|
| Length | `length` | 6 |
| TTL | `time-to-live` | 2 min |
| Block | `block-duration` | 10 min |
| Max fails | `max-failure-count` | 5 |

Fail limit → `TICKET_BLOCKED`. Re-send in TTL → `SEND_TICKET_TIME_LIMIT`.

Class map: `*TicketCacheService` ← `AbstractTicketCacheService`; `*TicketService` ← `AbstractTicketService` + `*Properties`. New flow = two `CacheName`s + those extensions + yml.

---

## Catalog

- `code` server-only: `{categoryId}-{product_code_seq}` (set in `ProductService.create`, ignored by mapper).
- Images = base64 in DB (`main_image_data`; `product_other_image` rows). Delete OTHER by `imageId`.
- Available = `inventoryCount > 0` (not `inventoryStatus`).

---

## Cart

JWT only; principal `userId` — never from body. No `cart` table: rows are `cart_item` by `user_id`. Empty cart = empty list (no create). Unknown item id → `CART_ITEM_NOT_FOUND`. Clear is idempotent.

- Merge key `(userId, productId, variantType[, variantValue])`.
- Variant must exist on product → else `PRODUCT_VARIANT_NOT_FOUND`.
- Stock = product-level `inventoryCount`; only `ACTIVE` → else `PRODUCT_NOT_AVAILABLE` / `INSUFFICIENT_STOCK`.
- Line prices snapshotted at add; response totals derived (`discountPrice` wins).

---

## Orders

```
POST /checkout | /checkout/guest     → RESERVED (stock −, reservedUntil +30m)
POST /orders/{id}/pay                → IPG initiate (RESERVED)
POST /orders/{id}/payment/confirm    → public → PAID + PAYMENT tx
POST /orders/{id}/cancel             → CANCEL_BY_USER (RESERVED|PAID)
POST /orders/{id}/receive            → RECEIVED (from SENDING)
POST /admin/orders/{id}/send|cancel  → ROLE_ADMIN
```

```
RESERVED → PAID → SENDING → RECEIVED
   ├─ timeout → FAILED (+ stock)
   └─ cancel  → CANCEL_BY_*   (also from PAID)
```

- Address + line snapshots at checkout.
- Cancel restores stock. After `SENDING` — no cancel.
- **PAID cancel:** `PaymentGateway.refund` + `REFUND` tx (needs IBAN). Missing IBAN → `USER_IBAN_REQUIRED` (order stays PAID). RESERVED cancel = no refund.
- Ledger: `order_transaction` (`PAYMENT` on confirm, `REFUND` on paid cancel) on order response as `transactions`.
- Gateway = `PaymentGateway` / `NoOpPaymentGateway` until real IPG. Scheduler: `ReservationReleaseJob` → `ReservationReleaseService` (`app.checkout.scheduling.enabled`).

---

## Reviews

Public list/summary; writes JWT. Admin `PATCH …/status` via `@PreAuthorize`.

- Aggregate derived on read (`PUBLISHED` only). One review per `(user, product)`.
- `rating` 1–5 required; title/comment optional. No purchase gate; `verifiedPurchase` snapshotted if user has PAID/SENDING/RECEIVED line for product.
- `authorName` snapshotted. New = `PUBLISHED`; admin can `HIDDEN`. Owner deletes own; admin any. Product need only exist (not `ACTIVE`).

---

## Errors

```json
{ "errorCode": "…", "message": "…", "errorParams": {} }
```

`message` from bundles via `Accept-Language`. `errorParams` only for `VALIDATION_ERROR`. No stack traces.
