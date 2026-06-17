# Security Incident Manager — Session Handover

> **Cross-references:**
> - Full history of completed work, known fixes, and architectural decisions → `PROJECT_LOG.md`
> - Codebase rules, package structure, layering, testing, security, git rules → `ARCHITECTURE.md`

---

## Project Overview
Building a Security Incident Management System from scratch.
Guided step-by-step as a beginner project with tests written alongside every feature.

**Stack:** Spring Boot 3.5, Java 21, PostgreSQL 16, Flyway, Docker, JWT, MapStruct, Lombok
**Architecture:** N-Tier — Controller → Service → Repository → Database (with DTO + Mapper layers)
**Workspace:** `/Users/camilo/Desktop/incident-manager`
**GitHub repo:** security-incident-manager

---

## Rules (must follow every session)

- **🚫 ASSISTANT NEVER WRITES JAVA CODE TO DISK. EVER.** All `.java` files are written by the user. The assistant provides code in text blocks only — the user types it. No exceptions, even if the user says "just do it". The only time this rule was broken was during Task 19 setup. It must not happen again.
- Assistant MAY write to disk: `.sql`, `.yaml`, `.yml`, `.xml`, `.properties`, `.md` files only
- Always explain every class and what each piece of code does when providing Java code
- Write tests for everything — every class gets a test class, no exceptions
- Work in feature branches (`feat/`, `fix/`, `chore/`, `test/`)
- Commit messages: imperative mood, no special characters e.g. `feat: add incident service`
- **Always provide a PR body file** (`pr-body-<task>.md`) after every task is complete — delete it after the PR is merged
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
- **Update HANDOVER.md and PROJECT_LOG.md when a task is merged** — mark task ✅, update current state, update test count, move "next task" pointer
- **Always state the full file path when providing a new class** — e.g. `src/main/java/com/securityincidentmanager/auth/JwtTokenService.java`
- **After every new class is written, always give a full beginner-friendly explanation** — what the class does, what each method does, and why
- Never start a new sprint until the current one is fully merged
- When user asks for "summary and handover", always update both `HANDOVER.md` and `PROJECT_LOG.md` first, then commit
- **Before writing any new code, always read the relevant existing files first** — entity types, repository method signatures, and field names must be verified before use. Never assume.
- **Before writing any new test, always read the existing test files first** — match exact structure, imports, assertion style (AssertJ), and naming conventions
- **All new code must align to existing structure**: use same types (`LocalDateTime` not `Instant`), same method signatures, same field names. Mismatches cause compile errors.
- Follow `ARCHITECTURE.md` package structure at all times
- Reuse existing components — never create a duplicate

---

## Sprint Tracking
Sprints are tracked in `sprint/` folder:
- `sprint/active/` — current sprint
- `sprint/todo/` — upcoming sprints
- `sprint/completed/` — finished sprints
- `sprint/BOARD.md` — overview

---

## Current State
- **Active branch:** `feat/rbac` (Task 20 in progress)
- **92 tests passing**
- **Frontend:** may be added after backend is complete

### Sprint 4 progress
- Task 17: `JwtTokenService` ✅
- Task 18: `JwtAuthenticationFilter` + `CustomUserDetailsService` ✅
- Task 19: Auth endpoints + Refresh Token lifecycle ✅
- Task 20: RBAC + Pageable restoration ⬜ ← **START HERE**
- Task 21: Security tests ⬜

### Next task: Task 20 — RBAC + Pageable restoration

**Scope:**
1. `SecurityConfig.java` — add RBAC: ADMIN full access, ANALYST own incidents only
2. `IncidentRepository` — restore `Pageable` + `Page<Incident>` on `findAllByReporterAndDeletedAtIsNull` and `findAllByAssignedAnalystAndDeletedAtIsNull`
3. `IncidentService` — update `getByReporter` and `getByAnalyst` callers to accept and pass `Pageable`
4. `IncidentController` — update the two endpoints to accept `Pageable`
5. `IncidentServiceTest` — update stubs from `List.of(...)` to `new PageImpl<>(...)` and method signatures to include `Pageable`

---

## Package Structure (quick ref — full detail in `ARCHITECTURE.md`)
```
com.securityincidentmanager
├── auth/            ← JwtTokenService, JwtAuthenticationFilter, CustomUserDetailsService,
│                      RefreshTokenService, AuthService
├── config/          ← SecurityConfig
├── controller/      ← IncidentController, UserController, AuthController
├── domain/
│   ├── entity/      ← User, Incident, RefreshToken
│   └── repository/  ← UserRepository, IncidentRepository, RefreshTokenRepository
├── dto/
│   ├── request/     ← IncidentCreateRequest, IncidentUpdateRequest,
│   │                   RegisterRequest, LoginRequest
│   └── response/    ← IncidentResponse, UserResponse, AuthResponse
├── exception/       ← ResourceNotFoundException
│                      (NotFoundException, ConflictException — Task 23)
├── mapper/          ← IncidentMapper, UserMapper
└── service/         ← IncidentService, UserService
```

---

## Upcoming Work
**Sprint 5 — Error Handling & Audit Logging** ⏸ BLOCKED until Sprint 4 merged
- Task 22: `GlobalExceptionHandler` (`@RestControllerAdvice`) — catch `ObjectOptimisticLockingFailureException` → 409
- Task 23: Custom exceptions (`NotFoundException`, `ConflictException`, etc.)
- Task 24: `@Version` on `Incident`, `V3__add_version_column.sql`, `hibernate-envers` + `@Audited`
- Task 25: Integration tests (`@SpringBootTest` + Testcontainers)
