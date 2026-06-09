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
- User writes all Java code themselves — always ask before making Java changes
- Always explain every class and what each piece of code does when providing Java code
- Assistant may edit config, XML, YAML, SQL without asking
- Write tests for everything — every class gets a test class, no exceptions
- Work in feature branches (`feat/`, `fix/`, `chore/`, `test/`)
- Commit messages: imperative mood, no special characters e.g. `feat: add incident service`
- Always show branch cleanup commands after every merge
- Delete branches after merging
- Reuse existing components — never create a duplicate
- Follow `ARCHITECTURE.md` package structure at all times
- Never start a new sprint until the current one is fully merged (see `sprint/BOARD.md`)

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
- `UserMapper` — `toResponse(User)` → UserResponse. All fields match by name, no @Mapping needed.
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

---

## Current State
- **Active branch:** `main` (Sprint 2 merged)
- **All 23 tests passing** — `mvn clean verify` → BUILD SUCCESS
- **Next task:** Start Sprint 3 — Services & Controllers

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

## Next sprint after merge: Sprint 3 — Services & Controllers

---

## Package Structure (follow exactly)
```
com.securityincidentmanager
├── auth/
├── config/
├── controller/
├── domain/
│   ├── entity/      ← User.java, Incident.java
│   └── repository/  ← UserRepository.java, IncidentRepository.java
├── dto/
│   ├── request/     ← IncidentCreateRequest.java, IncidentUpdateRequest.java
│   └── response/    ← IncidentResponse.java, UserResponse.java
├── exception/
├── mapper/          ← IncidentMapper.java, UserMapper.java
└── service/
```

---

## Upcoming Work (see sprint/todo/)
**Sprint 3 — Services & Controllers** ← START HERE after merge
- IncidentService + UserService + unit tests (Mockito)
- IncidentController + UserController + @WebMvcTest

**Sprint 4 — Security (JWT + RBAC)**
- JWT token service, JWT filter, auth endpoints
- Spring Security config, RBAC (ADMIN full access / ANALYST own incidents only)

**Sprint 5 — Error Handling & Audit Logging**
- GlobalExceptionHandler, custom exceptions
- Audit log table + Flyway migration
- Integration tests (@SpringBootTest + Testcontainers)
