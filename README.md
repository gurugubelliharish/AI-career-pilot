# AI Career Pilot

A personal AI recruiter that discovers jobs from multiple boards, scores them against your resume, and generates tailored application materials — all running locally in machine with no external AI API costs.

---

## What It Does

| Feature | Description |
|---|---|
| **Job Discovery** | Scrapes RemoteOK, Remotive, and Workable on a 5-minute schedule. Accumulates 1,800+ live listings. |
| **Resume Parsing** | Upload PDF or DOCX → extracts raw text via Apache PDFBox / Apache Tika |
| **Skill Extraction** | Passes resume text to a local Llama 3.2 model → returns a structured skill list |
| **Job Matching** | Scores every job: skill overlap (50%) + freshness decay (30%) + semantic goal alignment via embeddings (20%) |
| **AI Generation** | Generates a tailored resume or cover letter for any job using the local LLM |
| **Application Tracker** | Save jobs, mark applied, track status (saved → applied → interview → offer) |
| **Dashboard** | Live stats: total jobs, saved, applied, interviews, offers |

Everything runs on your machine inside Docker. Your resume data never leaves your system.

---

## Tech Stack

### Backend
| Technology | Version | Role |
|---|---|---|
| Java | 21 | Language |
| Spring Boot | 3.3.4 | Web framework, dependency injection, auto-configuration |
| Spring Security | (via Boot) | JWT stateless auth, BCrypt password hashing |
| Spring Data JPA + Hibernate | (via Boot) | ORM, repository pattern, paginated queries |
| Spring Scheduling | (via Boot) | `@Scheduled` background scan every 5 minutes |
| Spring Async | (via Boot) | `CompletableFuture` fire-and-forget scan endpoint |
| Spring Data Redis | (via Boot) | Rate-limit backoff state, Spring Cache infrastructure |
| LangChain4j | 0.36.0 | Java LLM abstraction — chat generation + embeddings via Ollama |
| Apache PDFBox | 3.0.2 | PDF text extraction |
| Apache Tika | 2.9.2 | DOCX / RTF / ODF text extraction |
| jjwt | (latest) | JWT token generation and validation |
| Lombok | (latest) | Boilerplate reduction (`@Builder`, `@Slf4j`, `@RequiredArgsConstructor`) |
| Flyway | (via Boot) | Database migration versioning |
| HikariCP | (via Boot) | JDBC connection pool (max 20 connections) |
| Maven | 3.9 | Build tool |

### Frontend
| Technology | Version | Role |
|---|---|---|
| React | 18.3 | UI component tree |
| TypeScript | 5.5 | Type safety |
| Vite | 5.4 | Build tool and dev server |
| TanStack Query | 5 | Server-state: caching, polling, background refetch, mutations |
| Zustand | 5 | Client-state: auth token persisted to localStorage |
| Axios | 1.7 | HTTP client with JWT interceptors and 401 auto-logout |
| React Router | v6 | SPA client-side routing with nested layouts |
| Tailwind CSS | 3 | Utility-first styling |
| lucide-react | (latest) | SVG icon library |

### Infrastructure
| Service | Technology | Role |
|---|---|---|
| Database | PostgreSQL 16 | Primary data store (jobs, users, profiles, applications) |
| Cache / State | Redis 7 | Workable cursor + rate-limit backoff, Spring Cache |
| AI Runtime | Ollama (latest) | Runs LLM models locally, no API key needed |
| LLM | Llama 3.2 (3B) | Skill extraction, resume/cover letter generation |
| Embeddings | nomic-embed-text | 768-dim text embeddings for semantic job matching |
| Reverse Proxy | nginx (alpine) | Single entry on port 80, routes `/api/*` → backend, `/` → SPA |
| Containers | Docker + Compose | Orchestrates all 6 services with health checks and named volumes |

---

## Architecture

```
Browser
  │
  └──► nginx :80
         ├── /api/*  ──► Spring Boot :8080
         │                    ├── PostgreSQL :5432
         │                    ├── Redis :6379
         │                    └── Ollama :11434
         └── /*      ──► React SPA (nginx-served static files)
```

- **No CORS issues** — browser sees one origin (port 80); nginx proxies API calls internally.
- **nginx strips `/api`** before forwarding to Spring Boot, so the backend's routes start at `/`.
- **Ollama** is a sidecar service. The `ollama-init` container pulls both models on first start and stores weights in a named Docker volume — subsequent starts skip the download.

---

## Project Structure

```
ai-career-pilot/
├── backend/
│   ├── Dockerfile                         # 2-stage: Maven build → JRE runtime
│   └── src/main/java/com/aicareer/jobradar/
│       ├── AiCareerPilotApplication.java  # @EnableScheduling @EnableAsync @EnableCaching
│       ├── config/                        # Redis, Security, CORS, RestTemplate, ScraperProperties
│       ├── security/                      # JwtTokenProvider, JwtAuthenticationFilter
│       ├── shared/                        # BaseEntity (UUID PK + audit), ApiResponse<T>, AppException
│       └── module/
│           ├── auth/                      # Register/Login, JWT issuance, BCrypt
│           ├── resume/                    # Upload, PDFBox/Tika parsing, LLM skill extraction
│           ├── profile/                   # CandidateProfile, skills, career goals, preferences
│           ├── discovery/                 # Job entity, scrapers, scheduler, JobDiscoveryService
│           │   └── scraper/
│           │       ├── JobScraper.java    # Strategy interface
│           │       ├── RemoteOKScraper.java
│           │       ├── RemotiveScraper.java
│           │       ├── WorkableScraper.java     # cursor pagination + rate-limit backoff
│           │       └── WorkableCursorStore.java # Redis-backed cursor + backoff state
│           ├── matching/                  # Skill overlap + freshness + embedding similarity scoring
│           ├── generator/                 # AI resume optimizer + cover letter via LangChain4j
│           ├── tracker/                   # JobApplication entity, status tracking, dashboard stats
│           └── notification/             # Telegram bot + email (stub)
├── frontend/
│   ├── Dockerfile                         # 2-stage: Node build → nginx static serve
│   └── src/
│       ├── api/
│       │   ├── client.ts                  # Axios instance with JWT + 401 interceptors
│       │   └── endpoints.ts               # Typed API functions per module
│       ├── store/
│       │   └── authStore.ts               # Zustand: token + user, persisted to localStorage
│       ├── viewmodels/                    # Custom hooks: TanStack Query + derived state per page
│       ├── pages/                         # Dashboard, Jobs, Resume, Profile, Matches, Generator, Tracker
│       ├── components/Layout/             # Sidebar, Header, Layout (with <Outlet>)
│       └── types/index.ts                 # Shared TypeScript interfaces
├── nginx/
│   └── nginx.conf                         # Reverse proxy config
└── docker-compose.yml                     # All 6 services + ollama-init model puller
```

---

## Getting Started

### Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (Mac/Windows) or Docker Engine + Compose (Linux)
- **8 GB RAM** minimum allocated to Docker (Ollama needs 4–6 GB for both models)
- Ports `80`, `8080`, `5432`, `6379`, `11434` free on your host

### 1. Clone

```bash
git clone https://github.com/DEEPAK__28/ai-career-pilot.git
cd ai-career-pilot
```

### 2. Configure environment (optional)

All values have safe defaults for local development. Copy and edit only if you need to override:

```bash
cp .env.example .env   # if the file exists, otherwise defaults in docker-compose.yml are used
```

Key variables:

| Variable | Default | Notes |
|---|---|---|
| `JWT_SECRET` | `change-me-in-production-...` | **Change before any public deployment** |
| `POSTGRES_PASSWORD` | `aicareer` | Fine for local; change in production |
| `REDIS_PASSWORD` | `aicareer` | Fine for local; change in production |
| `OLLAMA_CHAT_MODEL` | `llama3.2` | Swap to `llama3.1` or `mistral` if preferred |
| `SCAN_INTERVAL_MINUTES` | `5` | How often the background job scan runs |
| `WORKABLE_ENABLED` | `true` | Set to `false` to skip Workable (avoids 429s during dev) |

### 3. Start everything

```bash
docker compose up -d
```

**First start takes 5–15 minutes** — Docker pulls base images and `ollama-init` downloads `llama3.2` (~2 GB) and `nomic-embed-text` (~270 MB) into the `ollama_data` volume. Subsequent starts are instant.

Watch progress:

```bash
docker compose logs -f ollama-init   # model download progress
docker compose logs -f backend       # Spring Boot startup
```

### 4. Open the app

```
http://localhost
```

Register an account, upload your resume, and the system starts scraping and matching jobs immediately.

---

## How It Works

### Job Discovery Pipeline

```
JobScanScheduler (every 5 min)
  └── JobDiscoveryService.discoverJobsForProfile()
        └── for each JobScraper bean:
              scraper.scrape("", "")          ← fetch everything, no keyword filter
              saveNewJobs(jobs, source)        ← deduplicate by (source, externalJobId)
              freshnessService.calculateScore() ← attach 0–100 decay score
```

**Deduplication** is enforced at two levels: application-level check (`existsBySourceAndExternalJobId`) and a database unique constraint on `(source, external_job_id)`.

### On-Demand Scan (Refresh button)

```
POST /jobs/scan?source=WORKABLE&keywords=java,python
  └── Returns 202 Accepted immediately
  └── CompletableFuture.runAsync() → background thread
        └── For each keyword:
              WorkableScraper.scrape("java", "")   ← full pagination, no page cap
              saveNewJobs(...)
```

The frontend polls `GET /jobs` every 3 seconds while a scan is running, then stops after 60 seconds.

### Workable Cursor Crawl (Background)

Workable has 173K+ listings. The background scanner fetches **10 pages per run** (200 jobs) and saves the `nextPageToken` in Redis. The next scheduler run picks up where it left off. A full crawl takes ~24 hours across many 5-minute windows. On HTTP 429, a 30-minute backoff flag is set in Redis.

### Job Matching Score

```
priorityScore = (skillMatch × 0.50) + (freshness × 0.30) + (goalAlignment × 0.20)

skillMatch     = matched_skills / total_required_skills × 100
freshness      = max(0, 100 − (age_hours / 168 × 100))   ← 0 after 7 days
goalAlignment  = cosine_similarity(embed(careerGoals), embed(jobTitle + description[:500])) → [0, 100]
```

Embeddings are generated by `nomic-embed-text` running locally in Ollama.

---

## API Reference

All endpoints require `Authorization: Bearer <token>` except auth routes.

### Auth
| Method | Path | Body | Description |
|---|---|---|---|
| `POST` | `/auth/register` | `{name, email, password}` | Create account |
| `POST` | `/auth/login` | `{email, password}` | Returns JWT |

### Jobs
| Method | Path | Query Params | Description |
|---|---|---|---|
| `GET` | `/jobs` | `page=0&size=500` | List all active jobs (paginated) |
| `GET` | `/jobs/{id}` | — | Single job detail |
| `POST` | `/jobs/scan` | `source=WORKABLE&keywords=java,python` | Trigger background scan (202) |
| `GET` | `/jobs/matches` | `limit=30` | Top matched jobs for your profile |

### Profile
| Method | Path | Description |
|---|---|---|
| `GET` | `/profile` | Fetch candidate profile |
| `PUT` | `/profile/preferences` | Update role, location, career goals |
| `POST` | `/profile/skills/confirm` | Confirm inferred skills from resume |

### Resume
| Method | Path | Description |
|---|---|---|
| `POST` | `/resumes/upload` | Upload PDF/DOCX (multipart, max 20 MB) |
| `POST` | `/resumes/{id}/extract-skills` | Run LLM skill extraction on a resume |
| `GET` | `/resumes` | List uploaded resumes |
| `GET` | `/resumes/{id}/download` | Download resume file |

### Tracker
| Method | Path | Description |
|---|---|---|
| `GET` | `/tracker` | List saved/applied jobs |
| `GET` | `/tracker/stats` | Dashboard counts (saved, applied, interview, offer) |
| `POST` | `/tracker/save/{jobId}` | Save a job |
| `POST` | `/tracker/apply/{jobId}` | Mark as applied |
| `PATCH` | `/tracker/{id}/status?status=INTERVIEW` | Update application status |

### Generator
| Method | Path | Description |
|---|---|---|
| `POST` | `/generate/resume/{jobId}` | Generate tailored resume for a job |
| `POST` | `/generate/cover-letter/{jobId}` | Generate cover letter for a job |

---

## Scraper Status

| Source | Status | Mode | Notes |
|---|---|---|---|
| **Workable** | ✅ Active | Cursor-based incremental crawl + keyword search | 173K+ global jobs; rate-limited by API (429 backoff via Redis) |
| **RemoteOK** | ✅ Active | Full fetch | ~100 remote tech jobs per run; requires `User-Agent` header |
| **Remotive** | ✅ Active | Full fetch | ~100 remote jobs per run |
| **Greenhouse** | ⚠️ Disabled | N/A | No global search API — company-slug only |
| **Lever** | ⚠️ Disabled | N/A | No global search API — company-slug only |
| **Ashby** | ⚠️ Disabled | N/A | No global search API — company-slug only |
| **Naukri** | 🔧 Stub | Playwright (not wired) | Planned |

> **Why Greenhouse/Lever/Ashby are disabled:** These are ATS platforms, not aggregators. Their APIs only return jobs for a specific company you already know (e.g., `api.greenhouse.io/v1/boards/stripe/jobs`). There is no "give me all jobs across all companies" endpoint. Aggregators like Workable cover the same companies.

---

## Useful Commands

```bash
# Start all services in background
docker compose up -d

# Rebuild and restart backend (after Java changes)
docker compose up --build backend -d

# Rebuild and restart frontend (after React changes)
docker compose up --build frontend -d && docker compose up -d nginx

# Rebuild everything from scratch
docker compose up --build -d

# View live logs
docker compose logs -f backend
docker compose logs -f nginx        # see all API requests

# Stop everything (data volumes preserved)
docker compose down

# Stop and delete all data (fresh start)
docker compose down -v

# Database — connect to PostgreSQL
docker exec -it aicareer-postgres psql -U aicareer -d aicareer

# Database — count jobs by source
docker exec aicareer-postgres psql -U aicareer -d aicareer \
  -c "SELECT source, count(*) FROM jobs GROUP BY source ORDER BY count DESC;"

# Redis — check Workable rate-limit status
docker exec aicareer-redis redis-cli -a aicareer TTL workable:rate_limited

# Redis — manually clear rate limit (forces immediate Workable fetch)
docker exec aicareer-redis redis-cli -a aicareer DEL workable:rate_limited

# Redis — check pagination cursor position
docker exec aicareer-redis redis-cli -a aicareer GET workable:cursor:next_page_token

# Check all container health
docker compose ps
```

---

## Environment Variables Reference

| Variable | Default | Description |
|---|---|---|
| `JWT_SECRET` | `change-me-in-production-use-a-256-bit-key` | HMAC signing key for JWT — must be changed in production |
| `JWT_EXPIRATION_MS` | `86400000` | Token expiry in ms (default: 24 hours) |
| `POSTGRES_DB` | `aicareer` | Database name |
| `POSTGRES_USER` | `aicareer` | Database user |
| `POSTGRES_PASSWORD` | `aicareer` | Database password |
| `REDIS_PASSWORD` | `aicareer` | Redis auth password |
| `OLLAMA_CHAT_MODEL` | `llama3.2` | LLM for generation (swap to `mistral`, `llama3.1`, etc.) |
| `OLLAMA_EMBED_MODEL` | `nomic-embed-text` | Embedding model for semantic matching |
| `SCAN_INTERVAL_MINUTES` | `5` | Background scan frequency in minutes |
| `SCAN_ENABLED` | `true` | Set to `false` to disable background scanning |
| `WORKABLE_ENABLED` | `true` | Enable/disable Workable scraper |
| `REMOTEOK_ENABLED` | `true` | Enable/disable RemoteOK scraper |
| `REMOTIVE_ENABLED` | `true` | Enable/disable Remotive scraper |
| `CORS_ORIGINS` | `http://localhost` | Comma-separated allowed origins |
| `LOG_LEVEL` | `INFO` | Application log level (`DEBUG`, `INFO`, `WARN`) |

---

## Deployment Notes

This app is designed as a **local-first personal tool**. For public deployment:

1. **Change all secrets** — `JWT_SECRET`, `POSTGRES_PASSWORD`, `REDIS_PASSWORD` must not use defaults
2. **Enable Flyway** — set `FLYWAY_ENABLED=true` and `JPA_DDL_AUTO=validate` in production; never use `update` in prod
3. **Add HTTPS** — put Cloudflare or Let's Encrypt + Certbot in front of nginx
4. **Memory requirements** — Ollama needs ~4–6 GB RAM; minimum recommended VPS is 8 GB (e.g., Hetzner CX32 at ~€9/month)
5. **Firewall** — expose only ports 80 and 443; block 8080, 5432, 6379, 11434 from public internet

To replace Ollama with the OpenAI API (enables cheaper cloud deployment), swap `OllamaChatModel` and `OllamaEmbeddingModel` beans in `OllamaConfig.java` with their LangChain4j OpenAI equivalents — no other code changes required.

---

## License

MIT
