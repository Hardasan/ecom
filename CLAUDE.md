# Ecommerce Application

End-user e-commerce REST API — OTP-based authentication (SMS ticket system) plus password, a product catalog with image management, and a per-user shopping cart.

| Reference              | Link                                                         |
|------------------------|--------------------------------------------------------------|
| Business rules & flows | [.claude/rules/business.md](.claude/rules/business.md)       |
| Development rules      | [.claude/rules/development.md](.claude/rules/development.md) |

---

## Module Structure

```
src/main/java/com/ecommerce/
├── application/
│   ├── api/dto/
│   │   ├── cart/               # Cart request/response DTOs
│   │   ├── product/            # DTOs + product DTO enums (enumeration/)
│   │   ├── user/               # DTOs + user DTO enums (enumeration/)
│   │   └── wishlist/           # Wishlist request/response DTOs
│   ├── controller/cart/        # CartController
│   ├── controller/product/     # ProductController
│   ├── controller/user/        # UserController
│   ├── controller/wishlist/    # WishlistController
│   ├── service/cart/           # CartService, CartMapper
│   ├── service/product/        # ProductService, ProductMapper, ProductSpecifications
│   ├── service/wishlist/       # WishlistService, WishlistMapper
│   ├── service/ticket/         # AbstractTicketService, SignupTicketService, LoginTicketService
│   ├── config/                 # Spring config, security, properties
│   └── invoker/sms/            # SMS client
└── persistence/
    ├── entity/enumeration/     # Persistence-layer enums
    ├── repository/
    └── cache/                  # AppCacheManager, Caffeine/Redis implementations
```

---

## Tech Stack

| Layer        | Technology                                                                  |
|--------------|-----------------------------------------------------------------------------|
| Runtime      | Java 25 (CI toolchain must be 25)                                           |
| Web          | Spring Boot 4.1.0 — Spring MVC                                              |
| Security     | Spring Security + JWT (jjwt 0.13.0)                                         |
| Persistence  | Spring Data JPA + Hibernate 7.4.1.Final, PostgreSQL                         |
| Mapping      | MapStruct 1.6.3                                                             |
| Migrations   | Flyway (`baseline-on-migrate=false` — fresh DB only; V1, V1.1 … V1.5)       |
| Cache        | Caffeine (default) or Redis via Redisson 4.4.0 (`app.cache.type=redis`)     |
| Build        | Maven (single module)                                                       |
| ITest infra  | Testcontainers (PostgreSQL) + WireMock (SMS), JUnit 5                       |

---

## Cache

Two `AppCacheManager` implementations, wired via `@ConditionalOnProperty(app.cache.type)`:
- `caffeine` (default) — `CaffeineCacheManagerImpl`, per-entry TTL via `Expiry` + `ExpiringValue`
- `redis` — `RedisCacheManagerImpl`, Redisson `RBucket`

Both `RedissonAutoConfigurationV2` and `RedissonAutoConfigurationV4` are excluded in `application.yml`. `RedissonClient` only exists when `app.cache.type=redis`.

---

## Security

Public endpoints declared in `SecurityConfiguration.PUBLIC_ENDPOINTS[]`. Everything else requires `Authorization: Bearer <JWT>`.

Product write endpoints (`POST/PUT/DELETE /products/**`) require `ROLE_ADMIN`. `GET /products/**` is public.

JWT expiry: `security.jwt.expiration-time` (default `1h`). Secret key is committed — **rotate before prod**.
