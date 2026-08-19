# Development Rules

Non-obvious conventions only. Architecture → [CLAUDE.md](../../CLAUDE.md); domain → [business.md](business.md).

---

## Run

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
mvn verify              # ITests (Docker)
mvn verify -P release   # UTest + ITest
```

---

## Tests

- `*UTest`: JUnit 5 + Mockito; construct services in `@BeforeEach` — no `@InjectMocks` on services (OK on cache). No Spring context.
- `*ITest`: `@SpringBootTest` + `test` profile. Testcontainers PG, WireMock SMS `:9576`, Caffeine. Shared context; truncate `app_user` + reset WireMock per test. Scheduling off (`app.checkout.scheduling.enabled=false`); call `ReservationReleaseService` directly when testing expiry.

---

## Code

- Comments only for non-obvious *why*.
- Enums over string constants (DTO enums under `api/dto/<domain>/enumeration/`, persistence under `entity/enumeration/`).
- Search/filter → `@ModelAttribute *SearchRequestDto`, not loose `@RequestParam`s.
- Errors → `ExceptionParam` from `EcommerceException.getData()` only; never raw `Throwable`.
- Cache `put` always gets a real `Duration` (null/0 → wrong global default).

---

## MapStruct

Processor order: `lombok` → `lombok-mapstruct-binding` → `mapstruct-processor` → `hibernate-jpamodelgen`.

- `apply(dto, @MappingTarget)`: ignore server fields (`id`, `code`, timestamps, images).
- `@ElementCollection`: keep generated `clear`+`addAll` (PersistentBag identity).
- Child lists: add `toXxxDto(Child)` so parent `toResponseDto` maps them.

---

## Gotchas

- Redisson not auto-config; beans only if `app.cache.type=redis`. Both Redisson auto-configs excluded.
- `@Enumerated(STRING)` on enums inside `@Embeddable` / `@ElementCollection`.
- Spec map JSONB needs `@JdbcTypeCode(SqlTypes.JSON)`; ObjectMapper accepts case-insensitive enum keys.
- Images = base64 in DB columns, not files. `ProductOtherImage` / order children: `orphanRemoval` via parent collection.
- Order status **writes** → `*ForUpdate`. Release job → `pg_try_advisory_xact_lock` (other instances skip).
- Pay/refund → `Transaction` via order `addTransaction` (no orphan inserts).
- Flyway: `baseline-on-migrate=false`; never edit old files — add `V1.x__…`. Config is `.yml`.

---

## Frontend (Angular)

- Routes: **`loadComponent` only**. Never `import` a page component in `app.routes.ts`. First paint must not download cart/checkout/login/product/search.
- No `withPreloading` / `PreloadAllModules`.
- Never `import` images/fonts into TypeScript (that inlines them into the JS bundle). URLs under `/assets/…` only.
- Catalog photos live in the API/DB, not in the frontend bundle.
- Styles: SCSS (`styleUrl: './x.scss'`). New components use `"style": "scss"`.