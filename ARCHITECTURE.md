# Incident Manager — Architecture & Development Rules

## Tech Stack
| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| ORM | Hibernate / Spring Data JPA |
| Security | Spring Security + JWT (jjwt 0.12) |
| Mapping | MapStruct |
| Boilerplate | Lombok |
| Testing | JUnit 5 + Testcontainers + Spring Security Test |
| Build | Maven |
| Containerization | Docker / Docker Compose |

---

## Package Structure
```
com.securityincidentmanager
├── auth/          ← login, register, JWT filter, token service
├── config/        ← Spring Security config, beans, app-wide config
├── controller/    ← REST endpoints only, no business logic
├── domain/
│   ├── entity/    ← JPA entities (one per DB table)
│   └── repository/← Spring Data JPA repositories
├── dto/
│   ├── request/   ← inbound payloads (what the client sends)
│   └── response/  ← outbound payloads (what we send back)
├── exception/     ← custom exceptions + GlobalExceptionHandler
├── mapper/        ← MapStruct interfaces (entity <-> DTO)
└── service/       ← business logic, orchestration
```

---

## Layering Rules (N-Tier)
```
Controller → Service → Repository → Database
               ↑
            Mapper
              ↑
             DTO
```

- **Controllers** only accept request DTOs and return response DTOs. They never touch entities or repositories directly.
- **Services** contain all business logic. They call repositories and use mappers to convert between entities and DTOs.
- **Repositories** are interfaces only. No custom SQL unless absolutely necessary — use Spring Data query methods first.
- **Mappers** are MapStruct interfaces. No manual mapping code anywhere else in the codebase.
- **Entities** are never exposed outside the service layer. Never return an entity from a controller.

---

## Entity Rules
- Use `@Getter`, `@Setter`, `@NoArgsConstructor` — never `@Data` on entities.
- All primary keys are `UUID`, generated with `@GeneratedValue(strategy = GenerationType.UUID)`.
- Timestamps use `@CreationTimestamp` / `@UpdateTimestamp` — never set manually.
- Soft delete via `deleted_at` column — never hard delete incidents.
- Enums stored as `@Enumerated(EnumType.STRING)`.

---

## DTO Rules
- Request DTOs live in `dto/request/` and carry `@Valid` annotations for input validation.
- Response DTOs live in `dto/response/` and never expose passwords or sensitive fields.
- DTOs are records or plain classes — no JPA annotations.
- One DTO per use case (e.g. `IncidentCreateRequest`, `IncidentResponse`, `IncidentUpdateRequest`).

---

## Testing Rules
- Write tests alongside every new feature — not after.
- Every service method gets a unit test (Mockito, no Spring context).
- Every repository gets a `@DataJpaTest` with Testcontainers (real PostgreSQL).
- Every controller gets a `@WebMvcTest` with mocked service.
- Integration tests use `@SpringBootTest` + Testcontainers.
- Minimum line coverage: **70%** (enforced by JaCoCo — build fails below this).

---

## Security Rules
- All endpoints require authentication except `/api/auth/**` and `/actuator/health`.
- Role-based access: `ADMIN` can manage users and view all incidents. `ANALYST` can only manage their own assigned incidents.
- Passwords are always hashed with BCrypt — never stored in plain text.
- JWT access tokens expire in 15 minutes. Refresh tokens expire in 7 days.
- Never log passwords, tokens, or sensitive user data.

---

## Git & Branching Rules
- Branch naming: `feat/`, `fix/`, `chore/`, `test/`
- Commit messages: imperative mood, no special chars e.g. `"feat: add incident service and controller"`
- Every feature lives on its own branch.
- Delete branches after merging.
- PRs into `main` require all CI checks to pass.

---

## Quality Gates (enforced on every PR)
| Tool | What it checks |
|---|---|
| Checkstyle | Code style and formatting rules |
| SpotBugs + FindSecBugs | Static analysis for bugs and security issues |
| OWASP Dependency Check | Known CVEs in dependencies (fails on CVSS ≥ 7) |
| JaCoCo | Test line coverage ≥ 70% |

---

## Reuse Guidelines
- Before creating a new class, check if an existing one can be extended or reused.
- Shared validation logic goes in a utility class under `config/` or as a custom annotation.
- Common response structures (e.g. paginated responses, error responses) use shared DTO classes.
- No copy-paste code — extract to a method or shared component.
