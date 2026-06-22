# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## About the project

easyTask is an open-source task tracker inspired by Jira, but it must not copy Jira's implementation, UI, naming, or proprietary behavior.

It must support two deployment models from the same core codebase:
- self-hosted installation on a user/server machine
- hosted Cloud version

Performance, maintainability, and simple deployment are core requirements. When proposing architecture, libraries, or infra (auth, storage, multi-tenancy, billing, etc.), favor options that work in both deployment models — avoid baking in cloud-only assumptions (e.g. a hard dependency on a specific managed service) without an equivalent self-hosted path.

## Project state

Spring Boot 4.1.0 / Java 21. Implemented so far: full DB schema (Flyway), JPA entities, JWT-based authentication (register/login), Workspace (create/list/get/members/add-member), Project (create/list/get/members/add-member, auto-seeded defaults — see below, plus read-only `issue-types`/`statuses`/`issue-types/{id}/status-options` lookups for frontend dropdowns), Issue (create/list/get/update/change-status), and a minimal static frontend (see below). Stack: Spring Web MVC, Spring Data JPA, Spring Security, PostgreSQL, Flyway, Lombok, jjwt.

`ProjectDefaultsSeeder` runs inside `ProjectService.createProject` and gives every new project a working setup with zero manual config: a workspace-level `IssueType` catalog (Task/Bug/Story, created once per workspace and reused by every later project in it), all of them bound via `ProjectIssueType`, three `Status` rows (To Do/In Progress/Done), every type×status combination allowed via `ProjectIssueTypeStatus`, and a default `Board`+`BoardColumn`s. There's no admin CRUD yet for customizing issue types/statuses/boards — only this seeded default exists.

`Issue.number` is assigned via `ProjectRepository.incrementAndGetIssueSeq` — a native `UPDATE project SET issue_seq = issue_seq + 1 WHERE id = :id RETURNING issue_seq`, which both atomically increments under Postgres's row lock and returns the new value in one round trip (confirmed working against real Postgres, including concurrent-style back-to-back calls). The issue key shown to users (`PROJECT_KEY-number`) is computed in `IssueResponse`, never stored. `Issue.position` uses a fractional/LexoRank-style `NUMERIC` column with a fixed +1024 gap per insert/move (append-to-end only for now; no "insert between two cards" endpoint yet).

A minimal vanilla-JS frontend lives in `src/main/resources/static/` (`index.html`, `app.js`, `styles.css`) — no Node/build step, no framework; Spring serves it directly as static resources from the same origin/port as the API. This keeps deployment to a single artifact in both DC and Cloud. `app.js` does hash-based view routing (`#/login`, `#/workspaces`, `#/w/{id}`, `#/w/{id}/p/{id}`), keeps the JWT in `localStorage`, and gates buttons client-side using the `myRole` fields already on `WorkspaceResponse`/`ProjectResponse` — that gating is cosmetic only, the backend remains the actual permission boundary. `SecurityConfig` permits `/`, `/index.html`, `/*.js`, `/*.css`, `/favicon.ico` unauthenticated so the SPA shell can load before a token exists. Editing static files under `src/main/resources/static/` does **not** hot-reload under `spring-boot:run` — restart the app to see changes.

## DC vs Cloud (single codebase)

easyTask ships as one codebase running in two modes, controlled by Spring profile `dc` (default) or `cloud` (`SPRING_PROFILES_ACTIVE=cloud`, see `application-cloud.properties`). Differences between modes must be config/profile-gated behavior, not forked code.

- `app.registration.public-signup-enabled` — `false` for DC, `true` for Cloud. DC still allows exactly one bootstrap registration when the `users` table is empty (`AuthService.register`), so a fresh self-hosted install can create its first account without public signup.
- `Workspace` is the tenant boundary. Any authenticated user (DC or Cloud) can create a workspace and becomes its `OWNER` — no DC/Cloud branching needed there. The thing that *does* differ by mode is who can create a `User` account in the first place (see above).
- The highest-severity bug class here is a query/service that forgets to scope by `workspace_id`/membership — in Cloud that leaks one tenant's data to a stranger. `WorkspaceService` resolves resources only through `requireMembership(currentUser, workspaceId)`, and returns the same `WorkspaceNotFoundException` (404) whether the workspace doesn't exist or the caller just isn't a member — never reveal existence to non-members via a 403.

## Local dev environment

Datasource config (`application.properties`) reads `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` env vars with `localhost:5432` defaults. On this dev machine specifically, port `5432` is already taken by a native (non-Docker) PostgreSQL install — the project's own Postgres runs in Docker on **port 15432**:

```
docker run -d --name easytask-postgres -e POSTGRES_DB=easytask -e POSTGRES_USER=easytask -e POSTGRES_PASSWORD=easytask -p 15432:5432 postgres:16-alpine
DB_URL=jdbc:postgresql://localhost:15432/easytask DB_USERNAME=easytask DB_PASSWORD=easytask mvnw.cmd spring-boot:run
```

## Commands

Use the Maven wrapper (`mvnw.cmd` on Windows / `./mvnw` in bash) so the build doesn't depend on a locally installed Maven version.

```
mvnw.cmd clean package          # build
mvnw.cmd spring-boot:run        # run the app locally (needs DB_URL/DB_USERNAME/DB_PASSWORD, see above)
mvnw.cmd test                   # run full test suite (needs a running Postgres)
mvnw.cmd test -Dtest=EasyTaskApplicationTests              # run a single test class
mvnw.cmd test -Dtest=EasyTaskApplicationTests#contextLoads # run a single test method
```

There is no linter/formatter plugin configured in `pom.xml` yet.

## Architecture

Package-by-feature under `com.easytask`, one top-level package per business area — `auth`, `workspace`, `project`, `issue` — plus `common` for cross-cutting infrastructure with no single feature owner. Each feature package nests the same layer subpackages as before: `entity`, `repository`, `service`, `controller`, `dto`, `exception` (not every feature has all six — e.g. `common` has no `repository`/`controller`/`dto`).

- `common.entity` — `BaseEntity`/`CreatedOnlyEntity` mapped superclasses provide `id`/`createdAt`/`updatedAt`, marked `insertable = false, updatable = false` with Hibernate `@Generated` (DB triggers/defaults own these columns, not Java) — see gotcha below.
- `common.exception` — `AppException` (each subclass carries an `HttpStatus`), `GlobalExceptionHandler` maps them to responses, and `UserNotFoundException` (thrown by both `workspace` and `project`'s add-member flows, so it has no single feature owner).
- `common.security` — `JwtService`, `JwtAuthenticationFilter`, `SecurityConfig`.
- `common.config` — `@ConfigurationProperties` classes (`JwtProperties`, `RegistrationProperties`).
- `auth` — `User`/`UserStatus` entities, registration/login (`AuthService`, `AuthController`).
- `workspace` — `Workspace`/`WorkspaceMember`/`WorkspaceRole`, the tenant boundary (see below).
- `project` — `Project`/`ProjectMember`/`ProjectRole` plus the shared issue-taxonomy cluster (`IssueType`, `Status`, `Board`, `BoardColumn`, `ProjectIssueType`, `ProjectIssueTypeStatus`) — these have no single owner between `project` and `issue`, but `ProjectController` owns their lookup endpoints, so they live here.
- `issue` — `Issue`/`IssuePriority`/`IssueComment`/`IssueHistory` (the latter two unused so far — no repo/service/controller yet), create/list/get/update/change-status.

Within a feature, `controller` use `@AuthenticationPrincipal User` (from `auth.entity`) to get the caller (the JWT filter sets the `User` entity directly as the principal — no separate `UserDetails` wrapper); `dto` are request/response records validated with `jakarta.validation` annotations. Cross-feature service calls are direct repository/service references (e.g. `project.service.ProjectService` calls `workspace.repository.WorkspaceMemberRepository` directly, `issue.service.IssueService` reaches into `project.repository.*` and `auth.repository.UserRepository`) — no facades or interfaces between features, this is a flat layered architecture organized by feature for navigability, not a strict module boundary.

Schema is managed entirely by Flyway (`src/main/resources/db/migration/V1__init_schema.sql`); `spring.jpa.hibernate.ddl-auto=validate` only checks the JPA mapping against it, never generates DDL. `spring.jpa.open-in-view=false` — any service method that touches a lazy association after its repository call returns needs its own `@Transactional`/`@Transactional(readOnly = true)`, or it throws `LazyInitializationException`.

### Gotchas already hit once — don't re-debug these

- **`createdAt`/`updatedAt` read as `null` right after `save()`**: those columns are DB-owned (`insertable/updatable = false`) and Hibernate only refetches them via `@Generated` when the `INSERT`/`UPDATE` actually executes — which, without write-behind flushing, happens at transaction commit, *after* your method already built its response. Use `repository.saveAndFlush(entity)` when the response needs the value immediately.
- **Custom `@Component` `Filter` (e.g. `JwtAuthenticationFilter`) silently not authenticating**: Spring Boot auto-registers any `Filter` bean as a generic servlet filter *in addition to* wherever Spring Security's `addFilterBefore` places it. The generic copy runs before `SecurityContextHolderFilter` resets the per-request context, so authentication set there gets wiped before Security's own copy of the filter (skipped as "already filtered") would set it again. Fix: register a `FilterRegistrationBean<YourFilter>` with `setEnabled(false)` (see `SecurityConfig.disableAutoRegistration`).
- **A 404 on an unmapped endpoint shows up as 403 instead**: with no controller match, the container does an internal `ERROR` dispatch to `/error`, which re-enters the whole Spring Security chain with a fresh (unauthenticated) context. Without `.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()` in `SecurityConfig`, that second pass gets blocked by `anyRequest().authenticated()`, masking the real status.
- **`@Version` jumps by more than 1 per service-method call (e.g. 1→3 instead of 1→2)**: caused by mutating a managed entity's fields *before* calling a repository query method that touches the same table later in the same method. Hibernate's default `FlushModeType.AUTO` auto-flushes all pending dirty state before executing any JPQL/derived query, so the entity gets written once there (bumping `version`) and again at the method's own explicit `saveAndFlush` (bumping it a second time). Fix: run every repository read that the method depends on *before* mutating the entity, then apply all field mutations together right before the final `save`/`saveAndFlush` (see `IssueService.updateIssue`/`changeStatus`, where the assignee/position lookups were moved ahead of the `setX(...)` calls). This was caught by checking the actual `version` value in Postgres after a real API call, not by reading the code.