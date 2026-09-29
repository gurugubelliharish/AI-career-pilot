# AI Job Radar — Coding Guidelines

These guidelines apply to all code in this repository. They exist to keep the codebase
consistent and readable as the project grows. When in doubt, follow the patterns already
established in the scaffold rather than introducing new ones.

---

## General Principles

- Write code for the reader, not the compiler.
- Solve today's problem. Do not design for hypothetical future requirements.
- Three similar lines are better than a premature abstraction.
- Comments explain **why**, never what. Well-named identifiers already explain what.
- Delete code you don't use. Dead code misleads.

---

## Naming Conventions

### Java (Backend)

| Construct         | Convention          | Example                          |
|-------------------|---------------------|----------------------------------|
| Classes           | PascalCase          | `JobMatchingService`             |
| Interfaces        | PascalCase          | `JobScraper`                     |
| Methods           | camelCase (verb)    | `discoverJobs()`, `calculateScore()` |
| Variables         | camelCase           | `freshnessScore`, `resumeId`     |
| Constants         | SCREAMING_SNAKE_CASE| `MAX_RETRY_COUNT`                |
| Enums             | PascalCase + UPPER values | `ApplicationStatus.APPLIED` |
| Packages          | lowercase, no underscores | `com.aicareer.jobradar.module.auth` |
| REST endpoints    | kebab-case nouns    | `/tracker/save/{jobId}`          |

### TypeScript / React (Frontend)

| Construct           | Convention            | Example                           |
|---------------------|-----------------------|-----------------------------------|
| React components    | PascalCase            | `JobCard`, `Dashboard`            |
| Hooks / ViewModels  | camelCase with `use` prefix | `useJobsViewModel`          |
| Functions           | camelCase (verb)      | `saveJob()`, `handleConfirmAll()` |
| Variables           | camelCase             | `totalElements`, `uploadState`    |
| Constants (module)  | SCREAMING_SNAKE_CASE  | `STATUS_OPTIONS`, `STAT_CONFIG`   |
| TypeScript interfaces| PascalCase           | `JobsViewModel`, `CandidateProfile` |
| TypeScript types    | PascalCase            | `UploadState`, `ApplicationStatus`|
| Files (pages)       | PascalCase or index.tsx | `Dashboard/index.tsx`           |
| Files (viewmodels)  | camelCase with `use` prefix | `useJobsViewModel.ts`       |
| Files (components)  | PascalCase            | `Badge.tsx`, `Sidebar.tsx`        |

### Database (SQL / JPA)

| Construct         | Convention           | Example                        |
|-------------------|----------------------|--------------------------------|
| Tables            | snake_case (plural)  | `job_applications`, `candidate_skills` |
| Columns           | snake_case           | `freshness_score`, `posted_at` |
| Indexes           | `idx_{table}_{col}`  | `idx_jobs_source`              |
| Foreign keys      | `{table}_id`         | `user_id`, `profile_id`        |
| JPA entities      | PascalCase class, maps to snake_case table | `JobApplication` → `job_applications` |

---

## Frontend — MVVM Pattern

Every feature page follows the three-layer split:

```
ViewModel hook  →  owns all data fetching, mutations, derived state
Page component  →  consumes one ViewModel, renders only
Shared component→  pure presentational, accepts typed props
```

### ViewModel rules

- One ViewModel per page/feature. File name: `use{Feature}ViewModel.ts`.
- Always export the ViewModel interface alongside the hook.
- Return only what the View needs — no raw query objects, no `QueryResult`.
- Derived values belong in the ViewModel, not the View (e.g., `pendingSkills`, `savedJobIds`).
- Mutations expose simple action methods on the returned object (`saveJob(id)`, not `saveMutation.mutate`).

```ts
// correct
export interface JobsViewModel {
  jobs: Job[]
  saveJob: (jobId: string) => void
}

// incorrect — leaks implementation detail
export interface JobsViewModel {
  saveMutation: UseMutationResult<...>
}
```

### View (Page) rules

- Pages import exactly one ViewModel hook.
- No `useQuery` / `useMutation` / `useState` directly in pages (state for purely local UI like a modal open flag is the exception).
- No API calls directly in pages.
- Presentational config arrays (icon maps, color maps) are constants at the top of the file — not derived in the render.

### Component rules

- Shared components in `src/components/` accept only typed props — no hooks, no direct API calls.
- Use explicit prop interfaces, not inline types.

---

## Backend — Spring Boot Patterns

### Service layer

- Service method names are verbs: `upload()`, `match()`, `discoverJobs()`.
- Services own all business logic. Controllers are thin routing adapters.
- Services return domain objects or DTOs — never raw JPA entities to controllers when a DTO makes sense.
- Throw `AppException` for expected failures (`AppException.notFound()`, `AppException.conflict()`).
  Never let JPA exceptions bubble to the controller layer.

### Controllers

- Controllers map HTTP → service call → `ApiResponse<T>` wrapper. Nothing else.
- Use `@PreAuthorize` or SecurityContext for auth checks, not manual if-blocks.
- Return `ApiResponse.ok(data)` for success; let `GlobalExceptionHandler` handle errors.

### Entities

- All entities extend `BaseEntity` (UUID PK + `createdAt` / `updatedAt` via JPA auditing).
- JSONB columns use `@JdbcTypeCode(SqlTypes.JSON)` with `columnDefinition = "jsonb"`.
- `@ElementCollection` for simple string lists (skills, roles, locations) — no separate entity unless the list needs its own lifecycle.

### Repository layer

- One repository interface per entity, in the same module package.
- Prefer Spring Data method names over `@Query` where possible.
- Custom queries use `@Query` with JPQL (not native SQL) unless JPQL cannot express it.

---

## API Conventions

- All responses use the `ApiResponse<T>` envelope: `{ success, message, data, error, timestamp }`.
- Paginated responses wrap `PageResponse<T>` inside `ApiResponse`.
- HTTP status codes:
  - `200 OK` — successful read or update
  - `201 Created` — successful resource creation
  - `400 Bad Request` — validation failure
  - `401 Unauthorized` — missing or invalid token
  - `403 Forbidden` — authenticated but insufficient permission
  - `404 Not Found` — resource does not exist
  - `409 Conflict` — duplicate resource
  - `413 Payload Too Large` — file exceeds size limit
  - `500 Internal Server Error` — unexpected failure

---

## Error Handling

### Backend

- Business errors → `AppException` (maps to 4xx in `GlobalExceptionHandler`)
- Validation errors → `@Valid` on controller params, caught by `MethodArgumentNotValidException` handler
- Never swallow exceptions silently. Either rethrow or log + rethrow.
- Log at `ERROR` level only for unexpected failures (5xx). Expected failures (`AppException`) need no stack trace.

### Frontend

- API errors are caught in ViewModel hooks or in the `axios` response interceptor.
- 401 responses → auto logout + redirect to `/login` (handled in `src/api/client.ts`).
- Mutations expose an `error` state; Views display it inline near the relevant action.
- Never `console.error` in production paths — surface errors to the user or swallow intentionally with a comment.

---

## TypeScript Rules

- Prefer `interface` over `type` for object shapes.
- Avoid `any`. Use `unknown` + narrowing if the type is genuinely unknown.
- Explicit return types on all ViewModel hooks and API functions.
- No non-null assertions (`!`) without a comment explaining why it's safe.
- Prefer `??` over `||` for nullish defaults.

---

## React-Query Conventions

- Query keys are arrays: `['jobs']`, `['profile']`, `['tracker-stats']`.
- After a mutation, invalidate the related query key — do not manually update the cache.
- Stale time and cache time are not configured per-query (global defaults apply) unless there is a specific reason.

---

## Git Conventions

### Branch naming

```
feature/{short-description}     e.g. feature/naukri-scraper-selectors
fix/{short-description}         e.g. fix/skill-extraction-json-parse
chore/{short-description}       e.g. chore/upgrade-spring-boot
```

### Commit messages (Conventional Commits)

```
feat: add NaukriScraper CSS selectors for job card parsing
fix: handle null externalJobId in deduplication check
chore: upgrade Playwright to 1.45
refactor: extract JobMatchingService goal alignment to separate method
```

- Subject line: imperative mood, max 72 characters, no period.
- Body (optional): explain **why**, not what. Reference issue numbers if relevant.

---

## File & Folder Organisation

```
ai_docs/          Architecture and guidelines (this folder)
backend/          Spring Boot project (Maven)
frontend/         React + Vite project
  src/
    api/          HTTP client + typed endpoint functions
    types/        TypeScript interfaces (Model layer)
    viewmodels/   ViewModel hooks (one per feature)
    pages/        Page views (one folder per route)
    components/   Shared presentational components
    store/        Zustand stores
nginx/            nginx reverse proxy config
.github/          CI/CD workflows
```

- One class / component / hook per file.
- Co-locate test files next to source: `useJobsViewModel.test.ts` beside `useJobsViewModel.ts`.
- Do not put business logic in `index.ts` barrel files — they are re-exports only.
