# Security Incident Manager — Project Log

> **Cross-references:**
> - Current session state, active branch, next task, and rules → `HANDOVER.md`
> - Codebase rules, package structure, layering, testing, git rules → `ARCHITECTURE.md`

This file is append-only. Every completed task, known fix, and architectural decision is recorded here permanently. Never trim or rewrite history — only add to it.

---

## Completed Work

### Phase 1 — Project Setup ✅
- Spring Boot 3.5 project with Java 21
- `pom.xml` with all dependencies:
  - MapStruct 1.6.0, lombok-mapstruct-binding 0.2.0 (annotation processor order: Lombok → binding → MapStruct)
  - jjwt 0.12.6 (api/impl/jackson)
  - Testcontainers 1.20.1 (junit-jupiter + postgresql)
  - Quality plugins: Checkstyle 3.5.0, SpotBugs 4.8.6.4 + FindSecBugs 1.13.0, OWASP Dependency Check 12.1.3 (failBuildOnCVSS=7, failBuildOnError=false), JaCoCo 0.8.12 (70% minimum)
- `docker-compose.yml` — postgres:16-alpine, DB: incident_manager, user: appuser, password: apppassword
- `application.yaml` — datasource, JPA (ddl-auto: validate), Flyway, actuator
- `ARCHITECTURE.md` — documents all rules, package structure, layering, testing, security, git rules
- `lombok.config` — `lombok.addLombokGeneratedAnnotation = true` (JaCoCo excludes Lombok code)

### Phase 2 — Database Migration ✅
- `V1__init_schema.sql` — creates `users` and `incidents` tables
  - Uses `VARCHAR(20)` for role/priority/status (NOT PostgreSQL custom ENUMs — Hibernate incompatibility)
  - Soft delete via `deleted_at` column on incidents
  - Indexes on reporter_id, assigned_analyst_id, status, deleted_at

### Phase 3 — Domain Layer ✅
**Entities:**
- `User.java` — id (UUID), username, email, password, role (enum), createdAt, updatedAt. Inner enum: `Role { ADMIN, ANALYST }`
- `Incident.java` — id, title, description, priority (enum), status (enum), reporter (ManyToOne lazy), assignedAnalyst (ManyToOne lazy), createdAt, updatedAt, resolvedAt, deletedAt. Helper: `isDeleted()`. Inner enums: `Priority { LOW, MEDIUM, HIGH, CRITICAL }`, `Status { OPEN, IN_PROGRESS, RESOLVED, CLOSED }`
- Both use `@Getter @Setter @NoArgsConstructor` — never `@Data`

**Repositories:**
- `UserRepository` — `findByEmail`, `findByUsername`, `existsByEmail`, `existsByUsername`
- `IncidentRepository` — `findAllByDeletedAtIsNull(Pageable)`, `findAllByReporterAndDeletedAtIsNull`, `findAllByAssignedAnalystAndDeletedAtIsNull`, `findByIdAndDeletedAtIsNull(UUID id)`

**Tests:** 13 tests passing (`UserRepositoryTest` + `IncidentRepositoryTest`), `@DataJpaTest` + Testcontainers

### Sprint 2 — CI Fixes + DTOs & Mappers ✅
- `.github/workflows/ci.yml` — push/PR to main: postgres service → `mvn verify` → Checkstyle → SpotBugs
- `.github/workflows/security.yml` — OWASP runs weekly (Monday 08:00 UTC) + manual dispatch
- DTOs: `IncidentCreateRequest`, `IncidentUpdateRequest`, `IncidentResponse`, `UserResponse`
- Mappers: `IncidentMapper`, `UserMapper`

### Sprint 3 — Services & Controllers ✅
- `IncidentService` + `UserService` + `ResourceNotFoundException`
- `IncidentController` + `UserController`
- Full test coverage: `IncidentServiceTest`, `UserServiceTest`, `IncidentControllerTest`, `UserControllerTest`

### Sprint 4 — Security (JWT + RBAC) — Tasks 17–19 ✅

**Task 17 — JwtTokenService** (branch `feat/jwt-token-service`)
- `JwtTokenService` — generates and validates JWT access tokens (jjwt 0.12.6), `@Value` for secret and expiry, `generateToken(UserDetails, Map<String, Object>)`, `isTokenValid`, `extractUsername`, `getExpirationMs()`

**Task 18 — JwtAuthenticationFilter + CustomUserDetailsService** (branch `feat/jwt-filter`)
- `JwtAuthenticationFilter` extends `OncePerRequestFilter` — extracts Bearer token, validates, sets `SecurityContextHolder`
- `CustomUserDetailsService` implements `UserDetailsService` — loads `User` from DB, wraps in Spring Security `UserDetails`

**Task 19 — Auth endpoints + Refresh Token lifecycle** (branch `feat/auth-endpoints`)
- `V2__refresh_tokens.sql` — `refresh_tokens` table: `id UUID`, `token_hash VARCHAR(64) UNIQUE`, `user_id FK`, `expires_at`, `revoked BOOLEAN`, `created_at`; indexes on `user_id` and `token_hash`
- `RefreshToken` entity — `@ManyToOne(fetch = FetchType.EAGER)` on user (prevents LazyInitializationException)
- `RefreshTokenRepository` — `findByTokenHash`, `deleteAllByUser`
- `TokenPair` record — `(String accessToken, String refreshToken)`
- `RefreshTokenService` — 32-byte SecureRandom → Base64URL raw token, SHA-256 hex hash stored; `createRefreshToken`, `findByHash`, `rotate`, `revoke`, `revokeAllForUser`, `hash`
- `AuthService` — `register`, `login` (`@Transactional`); `refresh`, `logout` (NOT `@Transactional` — so `revokeAllForUser` commits before exception)
- `SecurityConfig` — CSRF disabled, STATELESS sessions, `/api/auth/**` + `/actuator/health|info` public, everything else authenticated
- `AuthController` — `/register` → 201, `/login` → 200, `/refresh` → 200, `/logout` → 204; refresh cookie: `HttpOnly; Secure; SameSite=Strict; Path=/api/auth; Max-Age=604800`
- `RegisterRequest`, `LoginRequest`, `AuthResponse` DTOs
- `application.yaml` — `jwt.expiration-ms: 900000` (15 min), `jwt.refresh-expiration-ms: 604800000` (7 days)
- **92 tests passing** after adding `@WithMockUser` to existing controller tests and `excludeAutoConfiguration = SecurityAutoConfiguration.class` to all `@WebMvcTest` classes

---

## Architectural Decisions

### Secure Refresh Token Design
- Raw opaque token (32 random bytes → Base64URL) is issued to client and set as `HttpOnly; Secure; SameSite=Strict` cookie — never in JSON body (XSS mitigation)
- Only the SHA-256 hex hash is stored in DB — prevents DB-dump token replay
- Token family invalidation: if a `revoked = true` token is presented to `/refresh`, `deleteAllByUser` nukes every session for the account → 401. The attacker's already-rotated token is also destroyed.
- `refresh()` and `logout()` in `AuthService` are deliberately NOT `@Transactional` — `revokeAllForUser` (which IS `@Transactional`) commits in its own transaction before the `IllegalStateException` is thrown, preventing rollback of the delete

### Pageable Restoration (planned — Task 20)
`findAllByReporterAndDeletedAtIsNull` and `findAllByAssignedAnalystAndDeletedAtIsNull` were simplified to `List<Incident>` during Sprint 3. Task 20 restores `Pageable` + `Page<Incident>` to prevent unbounded heap loads on large result sets.

### Optimistic Locking (planned — Task 24)
Add `@Version private Long version` to `Incident`. `GlobalExceptionHandler` catches `ObjectOptimisticLockingFailureException` → 409.

### Audit Logging via Hibernate Envers (planned — Task 24)
Add `hibernate-envers` + `@Audited` on `Incident`. Envers auto-creates `incidents_AUD` shadow table.

---

## Known Fixes (do not repeat these mistakes)

| Problem | Fix |
|---|---|
| `IsNull` query method took extra param | `findByIdAndDeletedAtIsNull(UUID id)` — no second param |
| Testcontainers Ryuk fails on Mac | `testcontainers.properties` with `ryuk.disabled=true` in `src/test/resources` |
| PostgreSQL custom ENUM incompatibility | Use `VARCHAR(20)` in migration, keep Java enums |
| `NoWhitespaceAtEndOfLine` not found in Checkstyle | Rule doesn't exist — remove it |
| `import jakarta.persistence.*` (star import) | Replace with explicit imports in both entities |
| Tab characters in source files | Replace with spaces |
| SpotBugs EI_EXPOSE_REP on JPA entity getters/setters | Add class to `spotbugs-exclude.xml` — false positive for JPA |
| SpotBugs EI_EXPOSE_REP2 on Spring `@Service`/`@Configuration` constructor injection | Add class to `spotbugs-exclude.xml` — SpotBugs doesn't understand Spring DI |
| OWASP URL column overflow | Upgrade OWASP plugin to 12.1.3 |
| OWASP NVD API returns null bytes on first run | Move OWASP to separate weekly `security.yml` workflow |
| NVD API key with special chars breaks shell | Pass via env var, not inline shell expansion |
| MapStruct `@Mapper` on a class | Must be `interface` |
| MapStruct "no write accessor" on response DTO | Add `@Setter` |
| IntelliJ ECJ overwrites Maven-compiled `*MapperImpl.class` | Redirect IntelliJ output to `out/production/classes` (Project Structure → Modules → Paths). Also added `maven-antrun-plugin` with `chflags uchg` to lock `*MapperImpl.class` after compile. Always fully quit IntelliJ before `mvn clean verify`. |
| `Instant.now()` used instead of `LocalDateTime.now()` | All timestamps use `LocalDateTime` — always read the entity before writing service code |
| `IncidentRepository.save()` (capital I) | Java is case-sensitive — use the lowercase injected field name |
| `findAllByDeletedAtIsNull(id)` called in softDelete | Wrong method — use `findByIdAndDeletedAtIsNull(id)` for single-record lookup |
| Repo methods returning `Page` called without `Pageable` | Changed to `List<Incident>` temporarily in Sprint 3 — restoring `Pageable` in Task 20 |
| `Page` result streamed without `.getContent()` | Call `.getContent()` before `.stream()` |
| Duplicate string literals | Define `private static final String` constants at the top of the class |
| Repository method signature changed but test not updated | Update test call, result type, and assertions together |
| Mockito self-attaching warning on JDK 21 | Add `maven-surefire-plugin` with `@{argLine} -javaagent:...mockito-core.jar` |
| Checkstyle `NeedBraces` + `LeftCurly` | Always use full block style with `{` on same line, body on next line |
| `@MockBean` deprecated since Spring Boot 3.4 | Use `@MockitoBean` from `org.springframework.test.context.bean.override.mockito` |
| `@WebMvcTest` loads Spring Security by default | Add `excludeAutoConfiguration = SecurityAutoConfiguration.class` to every `@WebMvcTest` class |
| `@WebMvcTest` fails because `JwtAuthenticationFilter` needs `JwtTokenService` | Add `@MockitoBean JwtTokenService` and `@MockitoBean CustomUserDetailsService` to every controller test |
| Refresh token returned in JSON body | Never — write it as `HttpOnly; Secure; SameSite=Strict` cookie only |
| Storing raw refresh token in DB | Store SHA-256 hex hash only (`token_hash VARCHAR(64)`) |
| Revoking one token on theft detection | Call `deleteAllByUser(user)` to nuke every session, then return 401 |
| Flyway migrations silently ignored in tests | `@SpringBootTest` and `@DataJpaTest` both run Flyway — a SQL syntax error fails all tests before any test method runs |
| `@WebMvcTest` POST endpoints return 403 even with `permitAll()` | Without `excludeAutoConfiguration = SecurityAutoConfiguration.class`, Spring Boot's CSRF protection blocks all POST requests. Every `@WebMvcTest` must exclude it. |

---

## IntelliJ ECJ Issue (resolved)

### Symptoms
`UserMapperTest` and `IncidentMapperTest` fail at construction with `Unresolved compilation problems`. IntelliJ's ECJ annotation processor rewrites `*MapperImpl.class` with broken bytecode.

### Root cause
IntelliJ's background auto-build uses ECJ, which cannot resolve project imports in the `target/generated-sources` context. It overwrites Maven's correct output.

### Fix applied
**Option C** — IntelliJ output paths redirected to `out/production/classes` and `out/test/classes` (Project Structure → Modules → Paths). IntelliJ and Maven now compile to separate directories. Additionally, `maven-antrun-plugin` locks `*MapperImpl.class` with `chflags uchg` after compile as a safety net.
