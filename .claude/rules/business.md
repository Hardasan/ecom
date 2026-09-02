# Business Rules

Non-obvious domain decisions only. Architecture → [CLAUDE.md](../../CLAUDE.md).

---

## User

- Mobile lives in both `username` (Security) and `mobile`.
- Login requires `isEnabled` **and** `isRegistered`.
- No single `name` column — use `firstName` + `lastName`.
- `iban` (`IR` + 24 digits): `GET/PUT /user/iban`. Optional.

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
POST /orders/{id}/pay                → JWT → IPG initiate (RESERVED)
POST /orders/{id}/payment/confirm    → public → PAID + PAYMENT tx
POST /orders/{id}/cancel|receive
POST /admin/orders/{id}/send|cancel|refund
GET  /admin/orders/refundable        → cancelled + PAYMENT + no REFUND
POST /warehouse/orders/{id}/approve|ship|deliver|cancel   → WAREHOUSE|ADMIN (fulfillment)
GET  /warehouse/orders[/{id}]        → WAREHOUSE|ADMIN (same projection as /admin/orders)
```

```
RESERVED → PAID → PROCESSING → SENDING → RECEIVED
   ├─ timeout → FAILED (+ stock)          (only from RESERVED)
   └─ cancel  → CANCEL_BY_*   (from RESERVED / PAID / PROCESSING, never after SENDING)
   COD: RESERVED → PROCESSING (COD never times out; reservedUntil stays null)
```

- Snapshots at checkout. Cancel restores stock; no cancel after `SENDING` (`PROCESSING` is still cancellable).
- **Guest:** `/checkout/guest` creates unregistered user + order, **no JWT**. Pay after signup/login with the same mobile.
- **Refund:** cancel never auto-refunds. Admin: list refundable → bank transfer outside → `POST …/refund` `{reference,iban}` → `REFUND` tx. Amount = order total.
- IPG: `PaymentGateway` / `NoOpPaymentGateway`. Expiry: `ReservationReleaseJob` → service (`app.checkout.scheduling.enabled`).
- **Warehouse fulfillment (`ROLE_WAREHOUSE`, admins allowed too):** `approve` PAID/COD-RESERVED → `PROCESSING` (stamps `approvedAt` + `fulfilledByUserId`); `ship` `PROCESSING` → `SENDING` (**requires** `carrier` + `trackingNumber`, stamps `shippedAt`); `deliver` `SENDING` → `RECEIVED` (staff-side twin of the buyer's `receive`); `cancel` = admin-cancel (restock, becomes refundable). Admin `POST /admin/orders/{id}/send` still short-circuits PAID/PROCESSING → `SENDING` without capturing tracking. Fulfillment columns are all nullable and fill in per step (`V1.27`). A paid order in `PROCESSING` counts as realised revenue and as a verified-purchase / active-discount state, exactly like `PAID`.

---

## Staff & Roles

- Single role per user (`AppUser.role`): `ROLE_APP_USER` (shopper), `ROLE_ADMIN`, `ROLE_WAREHOUSE` (fulfillment operator). Login (`POST /user/login`) returns the role; the dashboard host routes admins to `/admin`, warehouse staff to `/warehouse`.
- **Warehouse accounts are created by an admin**, not by signup: `POST /api/admin/staff` `{firstName,lastName,mobile,password}` → enabled + registered `ROLE_WAREHOUSE` user (mobile is the username). `GET /api/admin/staff` lists them; `PATCH …/{id}/status {enabled}` disables/enables a login (a disabled account fails `loadUserByUsername`, so both new logins **and** existing JWTs are rejected immediately); `POST …/{id}/reset-password {password}`. All admin-only; every mutation is scoped to `ROLE_WAREHOUSE` rows (an unknown-or-non-warehouse id → `USER_NOT_FOUND`), so it can never touch an admin or shopper account. Duplicate mobile → `USER_ALREADY_EXISTS`.

---

## Reviews

Public list/summary; writes JWT. Admin `PATCH …/status` via `@PreAuthorize`.

- Aggregate derived on read (`PUBLISHED` only). One review per `(user, product)`.
- `rating` 1–5 required; title/comment optional. No purchase gate; `verifiedPurchase` snapshotted if user has PAID/SENDING/RECEIVED line for product.
- `authorName` snapshotted. New = `PUBLISHED`; admin can `HIDDEN`. Owner deletes own; admin any. Product need only exist (not `ACTIVE`).

---

## Wishlist — Non-Obvious Rules

```
GET    /wishlist                        ← current user's wishlist (newest bookmark first)
POST   /wishlist/items                  ← productId  (idempotent add)
DELETE /wishlist/items/{itemId}         ← remove one bookmark by its id
DELETE /wishlist/products/{productId}   ← remove the bookmark for a product (heart-toggle off)
GET    /wishlist/products/{productId}   ← { inWishlist: true|false }  (heart-toggle state)
DELETE /wishlist                        ← clear all bookmarks (idempotent no-op when empty)
```

- All wishlist endpoints require a JWT (any role); they are **not** in `PUBLIC_ENDPOINTS`. The acting user is taken from the JWT principal (`UserDetailsDto.getId()`), never from the request body — a user can only touch their own wishlist.
- **There is no `wishlist` table.** A user's wishlist is simply the set of `wishlist_item` rows owned by that user (`wishlist_item.user_id`), mirroring the cart's user-keyed model. `WishlistResponseDto` is assembled from those rows and has **no** wishlist id.
- **A bookmark is keyed by `(userId, productId)`** (`uk_wishlist_item_user_product`) — product-level, **not** per-variant like the cart. A product is either on your wishlist or it is not.
- **Add is idempotent.** Adding a product already present is a no-op (no error, no duplicate row); the response is the unchanged wishlist. The bookmark is immutable — there is no quantity, variant, price snapshot, or `updated_at`.
- **The idempotency is enforced by the database, not by an `exists()` pre-check** — `WishlistItemRepository.insertIfAbsent` is a single `INSERT … ON CONFLICT ON CONSTRAINT uk_wishlist_item_user_product DO NOTHING`. A read-then-save cannot hold the guarantee: two concurrent adds of the same product both observe "absent" and the loser hits the unique constraint, turning the documented no-op into a 500. Catching that violation instead is not viable — it marks the surrounding transaction rollback-only, so the read-back that renders the response would fail too.
- **Only `ACTIVE` products can be added** (else `PRODUCT_NOT_AVAILABLE`; unknown id → `PRODUCT_NOT_FOUND`). Unlike the cart, **stock is deliberately NOT checked** — saving an out-of-stock product to buy when it returns is the whole point of a wishlist. Each item carries derived catalog flags: `inStock` (inventory > 0) and `available` (`ACTIVE` **and** in stock) so a wishlist page can badge unavailable lines.
- **Removals must reference a bookmark the user owns** — `DELETE /wishlist/items/{id}` (by bookmark id) and `DELETE /wishlist/products/{productId}` (by product) both return `WISHLIST_ITEM_NOT_FOUND` when the user has no matching row. `GET /wishlist/products/{productId}` is a pure membership check: it returns `{ inWishlist: false }` for an absent or even non-existent product, never an error.
- **No server-side "move to cart".** Moving a wishlisted product into the cart is just the existing `POST /cart/items` (which needs a `variantType` the wishlist does not carry) followed by an optional `DELETE /wishlist/products/{id}` — composed by the client, not a dedicated endpoint.

---

## Product Reviews & Ratings — Non-Obvious Rules

```
GET    /products/{productId}/reviews                  ← public, paginated (sort / rating / verifiedOnly filters)
GET    /products/{productId}/reviews/summary          ← public, average + count + 1..5 star histogram
POST   /products/{productId}/reviews                  ← create own review (JWT)
PUT    /products/{productId}/reviews/{reviewId}       ← edit own review (JWT, owner)
DELETE /products/{productId}/reviews/{reviewId}       ← delete (owner, or any review as ROLE_ADMIN)
PATCH  /products/{productId}/reviews/{reviewId}/status ← approve / hide: PENDING | PUBLISHED | HIDDEN (ROLE_ADMIN)
```

- **No rating aggregate is stored on the product.** A product's rating is derived on read from its `product_review` rows (same "derive, don't store" model as the cart total). `GET .../summary` computes `averageRating` (scale 1, `HALF_UP`; `0.0` when empty), `totalCount`, and a zero-filled `ratingCounts{1..5}` from a **single grouped query over `PUBLISHED` rows only**.
- **One review per `(user_id, product_id)`** (`uk_product_review_user_product`) — product-level, not per-variant. A second `POST` returns `PRODUCT_REVIEW_ALREADY_EXISTS`; edits go through `PUT`.
- **Reads are public via the existing `GET /api/products/**` rule** — no change to `PUBLIC_ENDPOINTS`. Writes fall through to `/api/**` authenticated. Admin moderation is **method-level** (`@PreAuthorize` on `PATCH .../status`), never path-based, so a normal user's `POST`/`PUT`/`DELETE` is unaffected.
- **`rating` is required (1–5); `title`/`comment` are optional** — a bare rating with no text is a valid review. The range is enforced by bean validation → `VALIDATION_ERROR`.
- **Any signed-in user may review; there is no purchase gate.** `verifiedPurchase` is only a badge, snapshotted at create time from `OrderRepository.existsPaidOrderForProduct` (the user has a **PAID** order line for that product). It is not recomputed on edit.
- **`authorName` is snapshotted** from the author's name at post time (like order snapshots); editing the review does not refresh it, nor does the user later renaming themselves.
- **Approval before publish.** New reviews start `PENDING` — invisible to the public and excluded from the average — until an admin approves them via `PATCH .../status = PUBLISHED`. An admin can also set `HIDDEN` (reject/remove) or delete any review; the owner can delete only their own. Non-owned mutations / unknown ids return `PRODUCT_REVIEW_NOT_FOUND`. **Editing a review sends it back to `PENDING`** (the changed content is re-approved). Admins see every status and can pull the moderation queue with `?status=PENDING`; everyone else sees `PUBLISHED` only.
- **Reviewing only requires the product to exist**, not to be `ACTIVE`, so a buyer can still review a discontinued item.
---

## Errors

```json
{ "errorCode": "…", "message": "…", "errorParams": {} }
```

`message` from bundles via `Accept-Language`. `errorParams` only for `VALIDATION_ERROR`. No stack traces.
