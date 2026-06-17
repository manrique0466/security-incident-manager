# Security Incident Manager — Session Handover

## Project Overview
Building a Security Incident Management System from scratch.
Guided step-by-step as a beginner project with tests written alongside every feature.

**Stack:** Spring Boot 3.5, Java 21, PostgreSQL 16, Flyway, Docker, JWT, MapStruct, Lombok
**Architecture:** N-Tier — Controller → Service → Repository → Database (with DTO + Mapper layers)
**Workspace:** `/Users/camilo/Desktop/incident-manager`
**GitHub repo:** security-incident-manager

---

## Rules (must follow every session)
- **🚫 ASSISTANT NEVER WRITES JAVA CODE TO DISK. EVER.** All `.java` files are written by the user. The assistant provides code in text blocks only — the user types it. This applies to every file, every session, no exceptions, even if the user says "just do it" or "finish this". The only time this rule was broken was during Task 19 setup. It must not happen again.
- Assistant MAY write to disk: `.sql`, `.yaml`, `.yml`, `.xml`, `.properties`, `.md` files only
- Always explain every class and what each piece of code does when providing Java code
- Assistant may edit config, XML, YAML, SQL without asking
- Write tests for everything — every class gets a test class, no exceptions
- Work in feature branches (`feat/`, `fix/`, `chore/`, `test/`)
- Commit messages: imperative mood, no special characters e.g. `feat: add incident service`
- **Always provide a PR body** after every commit (What, Endpoints if any, Tests, Notes)
- **Always use this exact git flow every time — no exceptions:**
  ```bash
  git add .
  git commit -m "..."
  git push origin <branch>
  git checkout main
  git merge <branch>
  git push origin main
  git branch -d <branch>
  git push origin --delete <branch>
  ```
- Delete branches after merging (both local and remote)
- Reuse existing components — never create a duplicate
- **Always state the full file path when providing a new class** — e.g. `src/main/java/com/securityincidentmanager/auth/JwtTokenService.java`
- **After every new class is written, always give a full beginner-friendly explanation** — explain what the class does, what each method does, and why, as if explaining to a child learning to code
- Follow `ARCHITECTURE.md` package structure at all times
- Never start a new sprint until the current one is fully merged (see `sprint/BOARD.md`)
- When user asks for "summary and handover", always update HANDOVER.md first, then commit it
- **Before writing any new code, always read the relevant existing files first** — entity types, repository method signatures, and field names must be verified before use. Never assume — always check.
- **Before writing any new test, always read the existing test files first** — match their exact structure, imports, assertion style (AssertJ), and naming conventions. Same rule as for production code.
- **All new code must align to existing structure**: use the same types (e.g. `LocalDateTime` not `Instant`), same method signatures as defined in the repository interface, same field names as in the entity. Mismatches cause compile errors.

---

## Sprint Tracking
Sprints are tracked in `sprint/` folder:
- `sprint/active/` — current sprint
- `sprint/todo/` — upcoming sprints
- `sprint/completed/` — finished sprints
- `sprint/BOARD.md` — overview

---

## What Is Done

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
- `IncidentRepository` — `findAllByDeletedAtIsNull(Pageable)`, `findAllByReporterAndDeletedAtIsNull`, `findAllByAssignedAnalystAndDeletedAtIsNull`, `findByIdAndDeletedAtIsNull(UUID id)` ← IsNull needs NO extra param

**Tests:**
- `UserRepositoryTest` — 6 tests, `@DataJpaTest` + Testcontainers (real PostgreSQL 16), `@AutoConfigureTestDatabase(replace=NONE)`
- `IncidentRepositoryTest` — 7 tests, same pattern, covers all 4 custom query methods + soft-delete edge cases
- `src/test/resources/testcontainers.properties` — `ryuk.disabled=true` (fixes Mac Docker Desktop)

### Sprint 2 — CI Fixes + Tests + DTOs & Mappers ✅
**CI fixes:**
- `.github/workflows/ci.yml` — push/PR to main: postgres service → `mvn verify` → Checkstyle → SpotBugs
- `.github/workflows/security.yml` — OWASP runs weekly (Monday 08:00 UTC) + manual dispatch
- OWASP moved out of main CI because NVD API is flaky on first run (no local cache)
- NVD API key stored as GitHub secret `NVD_API_KEY`, passed via env var (not inline shell expansion)

**DTOs:**
- `IncidentCreateRequest` — title (@NotBlank, max 200), description, priority (@NotNull)
- `IncidentUpdateRequest` — all fields optional (null = don't update): title, description, priority, status, assignedAnalystId
- `IncidentResponse` — id, title, description, priority, status, reporterId (UUID), assignedAnalystId (UUID), createdAt, updatedAt, resolvedAt. Uses `@Getter @Setter @NoArgsConstructor @AllArgsConstructor`
- `UserResponse` — id, username, email, role, createdAt. Uses `@Getter @Setter @NoArgsConstructor @AllArgsConstructor`

**Mappers:**
- `UserMapper` — `toUserResponse(User)` → UserResponse. All fields match by name, no @Mapping needed.
- `IncidentMapper` — `toResponse(Incident)` with `reporter.id → reporterId`, `assignedAnalyst.id → assignedAnalystId`. `toEntity(IncidentCreateRequest)` ignores id, reporter, assignedAnalyst, status, timestamps.

---

## Known Fixes (do not repeat these mistakes)
| Problem | Fix |
|---|---|
| `IsNull` query method took extra param | `findByIdAndDeletedAtIsNull(UUID id)` — no second param |
| Testcontainers Ryuk fails on Mac | `testcontainers.properties` with `ryuk.disabled=true` — must be in `src/test/resources` not `src/main/resources` |
| PostgreSQL custom ENUM incompatibility | Changed to `VARCHAR(20)` in migration, kept Java enums |
| `NoWhitespaceAtEndOfLine` not found in Checkstyle | Rule doesn't exist — removed it |
| `import jakarta.persistence.*` (star import) | Replaced with explicit imports in both entities |
| Tab characters in IncidentManagerApplication.java | Replaced with spaces |
| SpotBugs EI_EXPOSE_REP on JPA entity getters/setters | Added `spotbugs-exclude.xml` — false positive for JPA |
| OWASP URL column overflow (VARCHAR 1000 too small) | Upgraded OWASP plugin from 10.0.3 to 12.1.3 |
| OWASP NVD API returns null bytes on first run | Moved OWASP to separate weekly `security.yml` workflow |
| NVD API key with special chars breaks shell | Pass via env var: `env: NVD_API_KEY: ${{ secrets.NVD_API_KEY }}` then `-Dnvd.api.key="${NVD_API_KEY}"` |
| MapStruct `@Mapper` on a class | Must be `interface`, not `class` |
| MapStruct "no write accessor" on response DTO | Add `@Setter` — MapStruct uses no-args constructor + setters by default |
| IntelliJ ECJ overwrites Maven-compiled `*MapperImpl.class` | **FIXED** — IntelliJ output path changed to `out/production/classes` (Project Structure → Modules → Paths). Also added `maven-antrun-plugin` to `pom.xml` using `chflags uchg` to lock `*MapperImpl.class` after compile (belt-and-suspenders, macOS-safe with `failonerror=false`). Always fully quit IntelliJ (Cmd+Q) before running `mvn clean verify`. |
| `Instant.now()` used instead of `LocalDateTime.now()` | All timestamp fields in entities use `LocalDateTime` — always read the entity before writing service code |
| `IncidentRepository.save()` (capital I) instead of `incidentRepository.save()` | Java is case-sensitive — class name vs injected field name. Always use the lowercase field name |
| `findAllByDeletedAtIsNull(id)` called in softDelete | Wrong method — use `findByIdAndDeletedAtIsNull(id)` for single-record lookup |
| Repo methods returning `Page` called without `Pageable` arg | `findAllByReporterAndDeletedAtIsNull` and `findAllByAssignedAnalystAndDeletedAtIsNull` changed to return `List<Incident>` — only `getAll` needs pagination |
| `Page` result streamed without `.getContent()` | `Page` is a wrapper — call `.getContent()` before `.stream()` to extract the list |
| Duplicate string literals repeated across methods | Define `private static final String` constants at the top of the class |
| Repository method signature changed but test not updated | When a repo method drops `Pageable`, update the test call, result type (`List` not `Page`), and assertions (`.size()` / `.isEmpty()` not `.getTotalElements()`) |
| Mockito self-attaching warning on JDK 21 | Added `maven-surefire-plugin` with `@{argLine} -javaagent:...mockito-core-${mockito.version}.jar` — preserves JaCoCo's argLine and registers Mockito as a proper agent |
| Checkstyle `NeedBraces` + `LeftCurly` on `if` statements | Always use full block style — `{` must be followed by a line break, body indented on next line, closing `}` on its own line |
| `@MockBean` deprecated since Spring Boot 3.4 | Use `@MockitoBean` from `org.springframework.test.context.bean.override.mockito.MockitoBean` instead |
| `@WebMvcTest` loads Spring Security by default | Add `excludeAutoConfiguration = SecurityAutoConfiguration.class` to disable it until JWT is implemented in Sprint 4 |
| `@WebMvcTest` fails because `JwtAuthenticationFilter` (`@Component`) needs `JwtTokenService` (`@Service` with `@Value`) | Add `@MockitoBean private JwtTokenService jwtTokenService` and `@MockitoBean private CustomUserDetailsService customUserDetailsService` to every controller test — these satisfy the filter's constructor without real beans |
| Unused imports in controller / controller test | Never import `Incident` in the controller (not referenced there). In tests, only import what the test itself uses — don't copy all imports from the production class |
| Refresh token returned in JSON body | **Never** put the refresh token in `AuthResponse` — XSS can steal it. Write it as an `HttpOnly; Secure; SameSite=Strict` cookie via `HttpServletResponse.addCookie()`. `/refresh` and `/logout` read it from `request.getCookies()` only |
| Storing raw refresh token in database | Always store the SHA-256 hex hash (`token_hash VARCHAR(64)`), never the plaintext opaque string. The raw string is a credential — treat it like a password |
| Revoking one token on theft detection is insufficient | If a `revoked = true` token is presented to `/refresh`, call `deleteAllByUser(user)` to wipe every session for that account, then return `401`. The attacker's rotated token is nuked |
| Assuming Flyway migrations are invisible to tests | Wrong — `@SpringBootTest` and `@DataJpaTest` both boot Flyway against the Testcontainers PostgreSQL instance. A syntax error in any `V*.sql` file fails all 62 tests at context load, before a single test method runs |
| `@WebMvcTest` POST endpoints return 403 even with `permitAll()` | Without `excludeAutoConfiguration = SecurityAutoConfiguration.class`, Spring Boot's CSRF protection remains active and blocks all POST requests — even on `permitAll()` paths. Every `@WebMvcTest` class in this project must include `excludeAutoConfiguration = SecurityAutoConfiguration.class`. |

---

## Architectural Adjustments (injected before Sprint 4 Task 19)

These four changes were agreed in the session after Tasks 17–18 merged. They are non-breaking — zero existing classes are rewritten, and all 62 tests continue to pass. They must be applied in the order listed.

### 1. Secure Token Revocation — Refresh Token Lifecycle (Sprint 4, Task 19)

**Problem:** Stateless JWTs cannot be revoked before expiry. A stolen token gives an attacker uninterrupted access until natural expiry.

**Agreed design:**
- Reduce access token lifetime to **15 minutes** (`jwt.expiration-ms: 900000`). `JwtTokenServiceTest` hardcodes its own expiry value and is unaffected.
- Add `jwt.refresh-expiration-ms: 604800000` (7 days) to `application.yaml`.
- Introduce a `refresh_tokens` table (Flyway `V2`) and a `RefreshToken` entity.
- Issue the refresh token in an **`HttpOnly`, `Secure`, `SameSite=Strict` cookie** — never in the JSON body. This prevents XSS from stealing the long-lived credential.
- `/refresh` and `/logout` read the refresh token from the cookie only — no request body field for it.
- On rotation: old token is marked `revoked = true`; new access JWT + new refresh cookie are issued.
- **Token family invalidation (theft detection):** if a token whose `revoked = true` is presented to `/refresh`, immediately call `deleteAllByUser(user)` to nuke every active session for that account, then return `401`. The attacker's already-rotated token is destroyed. The user must re-authenticate.
- Store the **SHA-256 hash** of the raw token in `token_hash`, never the plaintext — prevents DB-dump token replay.

**New files for Task 19:**
| File | Purpose |
|---|---|
| `src/main/resources/db/migration/V2__refresh_tokens.sql` | Creates `refresh_tokens` table |
| `domain/entity/RefreshToken.java` | JPA entity |
| `domain/repository/RefreshTokenRepository.java` | `findByTokenHash`, `deleteAllByUser` |
| `auth/RefreshTokenService.java` | create, validateAndRotate, revoke |
| `auth/AuthService.java` | register + login business logic |
| `controller/AuthController.java` | `/api/auth/register`, `/login`, `/refresh`, `/logout` |
| `dto/request/RegisterRequest.java` | username, email, password, role |
| `dto/request/LoginRequest.java` | email, password |
| `dto/response/AuthResponse.java` | accessToken, tokenType, expiresIn (no refreshToken field) |
| Tests for every class above | Match existing test structure |

**⚠️ Flyway warning:** `V2__refresh_tokens.sql` is executed by Flyway when `@SpringBootTest` and `@DataJpaTest` boot the Testcontainers database. A syntax error in this file will fail all 62 existing tests before a single test method runs. The script must be flawless PostgreSQL.

---

### 2. Query Pagination Restoration (Sprint 4, Task 20)

**Problem:** `findAllByReporterAndDeletedAtIsNull` and `findAllByAssignedAnalystAndDeletedAtIsNull` return plain `List<Incident>`, which loads unbounded result sets into the JVM heap.

**Fix:** Restore `Pageable` parameter and `Page<Incident>` return type on both methods. Update `IncidentService` callers and adjust any mock assertions in tests to use `.getContent()` where needed.

---

### 3. Optimistic Locking (Sprint 5, Task 24)

**Problem:** Concurrent analyst updates to the same incident silently overwrite each other (last-write-wins).

**Fix:** Add `@Version private Long version;` to `Incident.java`. Requires Flyway `V3__add_version_column.sql` (`ALTER TABLE incidents ADD COLUMN version BIGINT DEFAULT 0 NOT NULL`). `GlobalExceptionHandler` (Task 22) must catch `ObjectOptimisticLockingFailureException` and return `409 Conflict`.

---

### 4. Automated Audit Logging via Hibernate Envers (Sprint 5, Task 24)

**Problem:** Building a custom audit log requires significant boilerplate and risks missing changes as the app scales.

**Fix:** Add `org.hibernate.orm:hibernate-envers` to `pom.xml`. Annotate `Incident.java` with `@Audited`. Envers auto-creates `incidents_AUD` shadow table and logs every state change, user identity, and timestamp.

---

## Current State
- **Active branch:** `feat/auth-endpoints` (Sprint 4 Task 19 complete, pending merge)
- **92 tests passing** (0 failures, all JaCoCo coverage checks met)
- **Frontend:** may be added after backend is complete — keep this in mind when planning Sprint 5+

### Sprint 4 progress
- Task 17: `JwtTokenService` ✅ (branch `feat/jwt-token-service`, merged)
- Task 18: `JwtAuthenticationFilter` + `CustomUserDetailsService` ✅ (branch `feat/jwt-filter`, merged)
- Task 19: Auth endpoints + Refresh Token lifecycle ✅ (branch `feat/auth-endpoints`, **pending merge**)
- Task 20: Spring Security config + RBAC + Pageable restoration ⬜ ← **START HERE**
- Task 21: Security tests (`@WithMockUser`, forbidden/authorized paths) ⬜

### Next task: Task 20 — RBAC + Pageable restoration ← START HERE
```bash
# First merge Task 19 branch
git checkout main
git merge feat/auth-endpoints
git push origin main
git branch -d feat/auth-endpoints
git push origin --delete feat/auth-endpoints

# Then start Task 20
git checkout -b feat/rbac
```

**Task 20 scope:**
1. `SecurityConfig.java` — add RBAC: ADMIN gets full access, ANALYST can only access own incidents
2. Restore `Pageable` parameter + `Page<Incident>` return type on `findAllByReporterAndDeletedAtIsNull` and `findAllByAssignedAnalystAndDeletedAtIsNull` in `IncidentRepository`
3. Update `IncidentService` callers to pass `Pageable` argument
4. Update test mocks that stub these repository methods (`List` → `Page`, add `.getContent()` where needed)

---

## RESOLVED: IntelliJ ECJ overwrites Maven-compiled class files

### Symptoms
```
java.lang.Error: Unresolved compilation problems:
    The import com.securityincidentmanager.domain cannot be resolved
    The import com.securityincidentmanager.dto cannot be resolved
    UserMapper cannot be resolved to a type
```
Both `UserMapperTest` (3 tests) and `IncidentMapperTest` (6 tests) fail at construction.
`IncidentManagerApplicationTests` fails with `ClassNotFoundException: Incident`.

### Root cause
IntelliJ runs its own annotation processor (ECJ) in the background, which:
1. Picks up the `*MapperImpl.java` files MapStruct generated into `target/generated-sources/annotations/`
2. Compiles them with ECJ — but ECJ cannot resolve the project imports in that context
3. Writes broken class files with `java/lang/Error: Unresolved compilation problems` baked in to `target/classes/com/securityincidentmanager/mapper/`
4. These overwrite Maven/javac's correct output

Evidence — `strings` on `IncidentMapperImpl.class` shows:
```
Unresolved compilation problems:
    The import com.securityincidentmanager.domain cannot be resolved
(LIncident;)LIncidentResponse;   ← unqualified binary name, ECJ artifact
```

The generated `.java` source files are **correct** (MapStruct + javac produced them fine). Only the `.class` files are corrupted.

`delegateBuildToMaven = true` is already set in `.idea/workspace.xml` but only affects explicit IDE build actions — IntelliJ's background file-watch auto-build still fires independently with ECJ.

### Fix options (try in order)

**Option A — Disable IntelliJ auto-build (quickest)**
IntelliJ → Settings (⌘,) → Build, Execution, Deployment → Compiler
→ Uncheck **"Build project automatically"**
→ Apply, then run `mvn clean verify` from terminal.

**Option B — Disable IntelliJ annotation processing for this module (most reliable)**
Directly edit `.idea/compiler.xml`.
Change the annotation processing profile from `enabled="true"` to `enabled="false"`:
```xml
<profile name="Annotation profile for incident-manager" enabled="false">
```
Save, then run `mvn clean verify`. Maven handles all annotation processing — IntelliJ no longer touches `target/`.

**Option C — Redirect IntelliJ output away from `target/` (permanent, clean)**
IntelliJ → Project Structure (⌘;) → Modules → incident-manager → Paths tab
→ Change "Output path" to `out/production/classes`
→ Change "Test output path" to `out/test/classes`
→ Apply
Now IntelliJ and Maven compile to separate directories and can never conflict.

### Resolution summary
**Option C applied** — IntelliJ output paths redirected to `out/production/classes` and `out/test/classes`. IntelliJ and Maven now write to separate directories and can never conflict. Additionally, `maven-antrun-plugin` locks `*MapperImpl.class` files with `chflags uchg` immediately after compilation as an extra safety net.

---

## Package Structure (follow exactly)
```
com.securityincidentmanager
├── auth/            ← JwtTokenService, JwtAuthenticationFilter, CustomUserDetailsService
│                      RefreshTokenService (Task 19), AuthService (Task 19)
├── config/          ← SecurityConfig (Task 20)
├── controller/      ← IncidentController, UserController
│                      AuthController (Task 19)
├── domain/
│   ├── entity/      ← User.java, Incident.java
│   │                   RefreshToken.java (Task 19)
│   └── repository/  ← UserRepository.java, IncidentRepository.java
│                       RefreshTokenRepository.java (Task 19)
├── dto/
│   ├── request/     ← IncidentCreateRequest.java, IncidentUpdateRequest.java
│   │                   RegisterRequest.java, LoginRequest.java (Task 19)
│   └── response/    ← IncidentResponse.java, UserResponse.java
│                       AuthResponse.java (Task 19)
├── exception/       ← ResourceNotFoundException
│                      (NotFoundException, ConflictException — Task 23)
├── mapper/          ← IncidentMapper.java, UserMapper.java
└── service/         ← IncidentService, UserService
```

---

## Upcoming Work (see sprint/)

**Sprint 4 — Security (JWT + RBAC)** ← ACTIVE (`sprint/active/`)
- Task 17: `JwtTokenService` ✅
- Task 18: `JwtAuthenticationFilter` + `CustomUserDetailsService` ✅
- Task 19: Auth endpoints + Refresh Token lifecycle ✅ (pending merge)
- Task 20: Spring Security config + RBAC + Pageable restoration ⬜ ← NEXT
- Task 21: Security tests ⬜

**Sprint 5 — Error Handling & Audit Logging** ⏸ BLOCKED until Sprint 4 merged
- Task 22: `GlobalExceptionHandler` (`@RestControllerAdvice`) — must catch `ObjectOptimisticLockingFailureException` → `409`
- Task 23: Custom exceptions (`NotFoundException`, `ConflictException`, etc.)
- Task 24: Data integrity + audit logging
  - Add `@Version private Long version` to `Incident.java`
  - Flyway `V3__add_version_column.sql`
  - Add `hibernate-envers` dependency + `@Audited` on `Incident.java`
- Task 25: Integration tests (`@SpringBootTest` + Testcontainers validating audit layers and optimistic locks)
