# AI Job Radar — Architecture

## System Overview

AI Job Radar is a personal AI recruiter platform. It scans job boards in real time, matches discovered jobs
against your resume and preferences using local AI, and helps you generate tailored applications.

```
┌─────────────────────────────────────────────────────────────────────┐
│                          User Browser                               │
│                   React SPA  (port 80 via nginx)                    │
└───────────────────────────────┬─────────────────────────────────────┘
                                │ /api/*
                     ┌──────────▼──────────┐
                     │     nginx (80)       │  Reverse proxy
                     │  strips /api prefix  │
                     └──────────┬──────────┘
                                │ :8080
                     ┌──────────▼──────────┐
                     │  Spring Boot (8080)  │  REST API
                     │   Java 21 / JPA      │
                     └──┬──────┬──────┬────┘
                        │      │      │
              ┌─────────▼─┐ ┌──▼───┐ ┌▼──────────┐
              │ PostgreSQL │ │Redis │ │  Ollama    │
              │   (5432)   │ │(6379)│ │  (11434)   │
              └───────────┘ └──────┘ └────────────┘
```

## Docker Services

| Service    | Image                    | Port  | Role                              |
|------------|--------------------------|-------|-----------------------------------|
| `postgres` | postgres:16-alpine       | 5432  | Primary database                  |
| `redis`    | redis:7-alpine           | 6379  | Response cache, session store     |
| `ollama`   | ollama:latest            | 11434 | Local LLM (llama3.2 + nomic-embed)|
| `backend`  | custom (Maven build)     | 8080  | Spring Boot REST API              |
| `frontend` | custom (Vite + nginx)    | 80    | React SPA                         |
| `nginx`    | nginx:alpine             | 80    | Reverse proxy / entry point       |

## Backend Module Map

Base package: `com.aicareer.jobradar`

```
module/
├── auth/          User entity (UserDetails), JWT register/login
├── resume/        PDF + DOCX upload, Apache Tika parser, Ollama skill extraction
├── profile/       CandidateProfile, CandidateSkill, preference management
├── discovery/     Job entity, JobScraper interface, NaukriScraper, JobScanScheduler
├── matching/      MatchResult, JobMatchingService (skill overlap + freshness + goals)
├── generator/     ResumeOptimizerService + CoverLetterService (LangChain4j prompts)
├── notification/  NotificationService (async), TelegramBot (LongPolling)
└── tracker/       JobApplication entity, ApplicationTrackerService, dashboard stats

config/            SecurityConfig, RedisConfig, OllamaConfig, WebConfig (CORS + auditing)
security/          JwtTokenProvider, JwtAuthenticationFilter
shared/            BaseEntity (UUID + audit), ApiResponse<T>, AppException, GlobalExceptionHandler
```

## Frontend Architecture (MVVM)

```
src/
├── api/           Data access layer — axios client + typed endpoint functions
├── types/         Model layer — TypeScript interfaces mirroring backend DTOs
├── store/         Zustand auth store (persisted to localStorage)
├── viewmodels/    ViewModel layer — custom hooks per page feature
│   ├── useDashboardViewModel.ts
│   ├── useJobsViewModel.ts
│   ├── useResumeViewModel.ts
│   ├── useProfileViewModel.ts
│   └── useTrackerViewModel.ts
├── pages/         View layer — thin React components, consume one ViewModel each
│   ├── Dashboard/
│   ├── Jobs/
│   ├── Resume/
│   ├── Profile/
│   ├── Tracker/
│   └── Auth/
└── components/    Shared presentational components (Badge, Layout, etc.)
```

### MVVM Responsibilities

| Layer       | Responsibility                                           | Allowed to import      |
|-------------|----------------------------------------------------------|------------------------|
| Model       | Shape of data (interfaces, types)                        | nothing                |
| API         | HTTP calls, typed return types                           | types, api/client      |
| ViewModel   | Queries, mutations, derived state, action handlers       | api, types, store      |
| View (Page) | Render only — consume ViewModel, emit events             | viewmodels, components |
| Component   | Reusable presentational UI, accepts props only           | types                  |

## Data Flow

### Resume → Skills

```
User uploads PDF/DOCX
  → ResumeService.upload() saves file to disk + DB
  → ResumeParserService.extractText() (PDFBox / Tika)
  → SkillExtractionService.extractSkillsAsJson() (Ollama llama3.2 prompt)
  → CandidateSkill records saved as inferred=true, confirmed=false
  → User reviews + confirms skills in Profile page
```

### Job Discovery → Match → Notify

```
JobScanScheduler (cron every N minutes)
  → iterates all complete CandidateProfiles
  → for each profile: calls JobDiscoveryService.discoverJobs(role, location)
    → each JobScraper.scrape() runs headless Playwright browser
    → deduplicates by (source, externalJobId), saves new Job records
  → FreshnessService.refreshFreshnessScores() updates all active jobs
  → JobMatchingService.match(profile, job) computes MatchResult
    └── priorityScore = 50% matchScore + 30% freshnessScore + 20% goalAlignment
  → NotificationService.notifyHighPriorityJob() for CRITICAL / HIGH results
    → Telegram message + Email (async, @Async)
```

## Security Model

- Stateless JWT — no server-side session
- Token signed with HMAC-SHA256, default expiry 24h
- `JwtAuthenticationFilter` (OncePerRequestFilter) runs on every request
- Public paths: `/auth/**`, `/actuator/health`, `/v3/api-docs/**`, `/swagger-ui/**`
- Role-based: `USER` (default), `ADMIN` reserved for future use
- Passwords hashed with BCrypt (strength 12)

## Database Schema Summary

| Table                | Key columns                                          |
|----------------------|------------------------------------------------------|
| `users`              | UUID PK, email (unique), role, notification flags    |
| `resumes`            | user FK, file metadata, raw_text TEXT, parsedData JSONB |
| `candidate_profiles` | user FK (unique), TEXT[] arrays, education JSONB     |
| `candidate_skills`   | profile FK, skillName (unique per profile), proficiency |
| `jobs`               | source + externalJobId (unique), requiredSkills TEXT[], freshnessScore |
| `job_applications`   | user + job (unique), status, matchScore, appliedAt   |
| `notifications`      | type, channel, sent boolean                          |

Flyway manages migrations. `V1__init_schema.sql` is the baseline.
Active profile: `dev` uses `ddl-auto: update` (no Flyway). `prod` uses Flyway only.

## AI Layer

| Capability         | Model              | Library       |
|--------------------|--------------------|---------------|
| Skill extraction   | llama3.2           | LangChain4j   |
| Resume optimization| llama3.2           | LangChain4j   |
| Cover letter gen   | llama3.2           | LangChain4j   |
| Embeddings         | nomic-embed-text   | LangChain4j   |

All models run locally via Ollama — zero API cost, fully private.
