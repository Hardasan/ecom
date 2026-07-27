# Ecommerce Application

End-user e-commerce REST API — OTP auth, product catalog, cart, orders (reservation → pay → ship), reviews.

| Reference | Link |
|-----------|------|
| Business rules | [.claude/rules/business.md](.claude/rules/business.md) |
| Development rules | [.claude/rules/development.md](.claude/rules/development.md) |

---

## Module Structure

```
src/main/java/com/ecommerce/
├── application/
│   ├── api/dto/{cart,product,order,user,wishlist}/
│   ├── controller/          # Cart, Product, User, Order, Checkout, WishlistController…
│   ├── service/{cart,order,payment,product,ticket,review,wishlist…}/
│   ├── config/              # security, properties
│   └── invoker/sms/
└── persistence/
    ├── entity/ (+ enumeration/)
    ├── repository/
    └── cache/               # Caffeine / Redis AppCacheManager
```

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Runtime | Java 25 |
| Web | Spring Boot 4.1 — MVC |
| Security | Spring Security + JWT (jjwt 0.13) |
| Persistence | Spring Data JPA, Hibernate 7.4, PostgreSQL |
| Mapping | MapStruct 1.6 |
| Migrations | Flyway (`baseline-on-migrate=false`; `V1`, `V1.1`, …) |
| Cache | Caffeine (default) or Redis/Redisson (`app.cache.type`) |
| Build / ITest | Maven; Testcontainers PG + WireMock SMS |

---

## Cache

`AppCacheManager` via `app.cache.type`: `caffeine` (default, per-entry TTL) or `redis` (Redisson `RBucket`). Redisson auto-config excluded; `RedissonClient` only when redis.

---

## Security

Public routes in `PublicEndPoint` (guest checkout, payment confirm — guest must signup/login before `/pay`). Else `Authorization: Bearer <JWT>`. Product writes / admin order ops → `ROLE_ADMIN`. `GET /products/**` public. JWT TTL `security.jwt.expiration-time` (default `1h`) — **rotate secret before prod**.
