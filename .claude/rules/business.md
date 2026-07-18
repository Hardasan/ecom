# Business Rules

Non-obvious domain decisions. Anything obvious from reading the code is not repeated here.
For architecture see [CLAUDE.md](../../CLAUDE.md).

---

## User Model

- Mobile number is stored in **both** `AppUser.username` (Spring Security) and `AppUser.mobile` (explicit lookups).
- `UserDetailServiceImpl` checks **both** `isEnabled` AND `isRegistered`. A user with `isRegistered=false` cannot log in even if their account exists.
- `firstName` + `lastName` — there is no `name` column.

---

## Authentication Flows

```
POST /user/signup-ticket            ← mobileNumber
POST /user/signup-ticket/validation ← mobileNumber + OTP  →  signupToken (UUID, short-lived)
POST /user/signup                   ← signupToken + firstName + lastName + password

POST /user/check-registration       ← mobileNumber  →  { isRegistered: true|false }

POST /user/login                    ← mobileNumber + password  →  JWT

POST /user/login-ticket             ← mobileNumber  (rejected if not isRegistered)
POST /user/login-ticket/validation  ← mobileNumber + OTP  →  JWT

POST /user/change-password          ← newPassword + confirmPassword  (JWT required)
```

`signupToken` is stored in `SignupCacheService` (cache `SIGNUP_TOKEN`) and proves OTP was completed.

`changePassword` does **not** verify the current password — `ChangePasswordRequestDto` has only `newPassword` + `confirmPassword`.

---

## OTP Rules

Signup and login flows are **completely isolated** — separate rate-limit counters, block lists, and cache buckets.

| Property       | Config key                            | Default |
|----------------|---------------------------------------|---------|
| Length         | `app.{flow}.ticket.length`            | 6       |
| TTL            | `app.{flow}.ticket.time-to-live`      | 2 min   |
| Block duration | `app.{flow}.ticket.block-duration`    | 10 min  |
| Max failures   | `app.{flow}.ticket.max-failure-count` | 5       |

After `max-failure-count` failures, the mobile is blocked for `block-duration` (`TICKET_BLOCKED`). Re-send within TTL is rejected with `SEND_TICKET_TIME_LIMIT`.

### OTP class map

```
CacheName: SIGNUP_TICKET / SIGNUP_LAST_TICKET  ←  SignupTicketCacheService
CacheName: LOGIN_TICKET  / LOGIN_LAST_TICKET   ←  LoginTicketCacheService
                    ↓
         AbstractTicketCacheService

SignupProperties / LoginProperties  ←  SignupTicketService / LoginTicketService
                    ↓
         AbstractTicketService (prepareTicket, sendTicketMessage, validateTicket)
```

To add a new OTP flow: add two `CacheName` values, extend `AbstractTicketCacheService` and `AbstractTicketService`, create a `*Properties` bean in `EcommercePropertiesConfiguration`, add config in `application.yml`.

---

## Product Catalog — Non-Obvious Rules

- **`code` is server-generated**, never from the client: `{categoryId}-{nextval('product_code_seq')}`. `ProductMapper.apply()` ignores it; set it after mapping in `ProductService.create()`.
- **Images are base64 stored in PostgreSQL**, not files. `mainImage` is embedded in the `product` row (`main_image_data TEXT`). `otherImages` are rows in `product_other_image`, each with its own `id`.
- **`ProductOtherImage` removal uses `imageId`**, not a list index. `DELETE /products/{id}/images?type=OTHER&imageId={imageId}`.
- **`isAvailable` filter** checks `inventoryCount` directly (`> 0` = available), not `inventoryStatus`.
- **`USER_LAST_TICKET` is gone** — renamed `SIGNUP_LAST_TICKET`. Stale cache keys with the old name are invalid after a flush.

---

## Shopping Cart — Non-Obvious Rules

```
GET    /cart                            ← current user's cart (created on first access)
POST   /cart/items                      ← productId + variantType + quantity
PATCH  /cart/items/{itemId}             ← quantity  (absolute set)
POST   /cart/items/{itemId}/increment   ← +1
POST   /cart/items/{itemId}/decrement   ← -1  (line removed when it would hit 0)
DELETE /cart/items/{itemId}             ← remove one line
DELETE /cart                            ← clear all lines
```

- All cart endpoints require a JWT (any role); they are **not** in `PUBLIC_ENDPOINTS`. The acting user is taken from the JWT principal (`UserDetailsDto.getId()`), never from the request body — a user can only touch their own cart.
- **There is no `cart` table.** A user's cart is simply the set of `cart_item` rows owned by that user (`cart_item.user_id`). `GET /cart` returns those rows (an empty cart when there are none — nothing is created); item mutations (`PATCH`/increment/decrement/`DELETE /cart/items/{id}`) that reference an id the user doesn't own return `CART_ITEM_NOT_FOUND`. `DELETE /cart` is an idempotent no-op when empty. The `CartResponseDto` is assembled from the rows (totals + `createdAt`/`updatedAt` derived from the items); it has **no** cart id.
- **A line is keyed by `(userId, productId, variantType)`** (`uk_cart_item_user_product_variant`). The same product in a different variant is a separate line. Re-adding the same product+variant **merges** into the existing line (quantities sum).
- **`variantType` must be one the product actually offers** — it must match a `Price.variantType` on the product, else `PRODUCT_VARIANT_NOT_FOUND`.
- **Stock is validated against `Product.inventoryCount`** (single product-level count; the catalog has no per-variant inventory). Add / increment / set-quantity that would push a line above `inventoryCount` returns `INSUFFICIENT_STOCK`. Only `ACTIVE` products are purchasable, else `PRODUCT_NOT_AVAILABLE`.
- **Price is snapshotted at add time** (`cart_item.unit_price` / `discount_price`) so the cart total is stable even if the catalog price later changes. `effectivePrice` / `lineTotal` / `totalPrice` in the response are derived; `discountPrice` wins over `unitPrice` when present.

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
PATCH  /products/{productId}/reviews/{reviewId}/status ← moderate: PUBLISHED / HIDDEN (ROLE_ADMIN)
```

- **No rating aggregate is stored on the product.** A product's rating is derived on read from its `product_review` rows (same "derive, don't store" model as the cart total). `GET .../summary` computes `averageRating` (scale 1, `HALF_UP`; `0.0` when empty), `totalCount`, and a zero-filled `ratingCounts{1..5}` from a **single grouped query over `PUBLISHED` rows only**.
- **One review per `(user_id, product_id)`** (`uk_product_review_user_product`) — product-level, not per-variant. A second `POST` returns `PRODUCT_REVIEW_ALREADY_EXISTS`; edits go through `PUT`.
- **Reads are public via the existing `GET /api/products/**` rule** — no change to `PUBLIC_ENDPOINTS`. Writes fall through to `/api/**` authenticated. Admin moderation is **method-level** (`@PreAuthorize` on `PATCH .../status`), never path-based, so a normal user's `POST`/`PUT`/`DELETE` is unaffected.
- **`rating` is required (1–5); `title`/`comment` are optional** — a bare rating with no text is a valid review. The range is enforced by bean validation → `VALIDATION_ERROR`.
- **Any signed-in user may review; there is no purchase gate.** `verifiedPurchase` is only a badge, snapshotted at create time from `OrderRepository.existsPaidOrderForProduct` (the user has a **PAID** order line for that product). It is not recomputed on edit.
- **`authorName` is snapshotted** from the author's name at post time (like order snapshots); editing the review does not refresh it, nor does the user later renaming themselves.
- **Auto-publish + reactive moderation.** New reviews are `PUBLISHED` and count immediately. An admin can set a review `HIDDEN` (removed from the public list **and** the average) or delete any review; the owner can delete only their own. Non-owned mutations / unknown ids return `PRODUCT_REVIEW_NOT_FOUND`. Admins see `HIDDEN` rows in the list; everyone else sees `PUBLISHED` only.
- **Reviewing only requires the product to exist**, not to be `ACTIVE`, so a buyer can still review a discontinued item.
---

## Error Response Contract

```json
{ "errorCode": "INVALID_TICKET", "message": "...", "errorParams": {} }
```

- `message` resolved from `messages.properties` / `messages_fa.properties` via `Accept-Language`. Falls back to the key.
- `errorParams` is `{ "fieldName": ["msg"] }` for `VALIDATION_ERROR`; `null` for others.
- Stack traces are **never** included.
