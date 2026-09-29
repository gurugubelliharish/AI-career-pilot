# AI Job Radar — Project Explainer (Personal Reference)

> This document is written for you to read at your own pace.
> It explains every technology, every decision, and every concept in plain language.
> No prior knowledge of any specific technology is assumed.

---

## Table of Contents

1. [What the project does (big picture)](#1-what-the-project-does)
2. [How the project is structured](#2-how-the-project-is-structured)
3. [Backend — Java & Spring Boot](#3-backend--java--spring-boot)
4. [Database — PostgreSQL, JPA, Flyway](#4-database--postgresql-jpa-flyway)
5. [Caching — Redis](#5-caching--redis)
6. [Security — JWT & Spring Security](#6-security--jwt--spring-security)
7. [AI Layer — Ollama, LangChain4j, Spring AI](#7-ai-layer--ollama-langchain4j-spring-ai)
8. [Document Parsing — PDFBox & Apache Tika](#8-document-parsing--pdfbox--apache-tika)
9. [Web Scraping — Playwright](#9-web-scraping--playwright)
10. [Notifications — Telegram & Email](#10-notifications--telegram--email)
11. [Frontend — React, TypeScript, Vite](#11-frontend--react-typescript-vite)
12. [Styling — Tailwind CSS](#12-styling--tailwind-css)
13. [Frontend Data Fetching — Tanstack Query](#13-frontend-data-fetching--tanstack-query)
14. [Frontend State — Zustand](#14-frontend-state--zustand)
15. [Frontend Routing — React Router](#15-frontend-routing--react-router)
16. [HTTP Client — Axios](#16-http-client--axios)
17. [Architecture Pattern — MVVM](#17-architecture-pattern--mvvm)
18. [Infrastructure — Docker & docker-compose](#18-infrastructure--docker--docker-compose)
19. [Reverse Proxy — nginx](#19-reverse-proxy--nginx)
20. [CI/CD — GitHub Actions](#20-cicd--github-actions)
21. [The 11 Modules — What Each One Does](#21-the-11-modules--what-each-one-does)
22. [How Everything Connects (end-to-end flow)](#22-how-everything-connects-end-to-end-flow)

---

## 1. What the project does

**AI Job Radar** is your personal job-hunting assistant that runs 24/7 on a server.

Here is the problem it solves: You want a job. You upload your resume. The system reads your resume, figures out what skills you have, and continuously scans job boards (like Naukri) in the background. When it finds a job that matches your skills, it scores it, ranks it, and sends you a Telegram message or email. You can also ask it to rewrite your resume specifically for that job, or generate a cover letter.

Everything runs on your own server. No data leaves your machine. The AI models run locally.

---

## 2. How the project is structured

The project has three main parts:

```
ai-career-pilot/
├── backend/      ← The server (Java application)
├── frontend/     ← The website (React application)
├── nginx/        ← The gatekeeper (routes browser requests)
├── ai_docs/      ← Documentation (you are here)
└── docker-compose.yml  ← Starts everything together
```

Think of it like a restaurant:
- **nginx** is the front door — customers (browsers) walk in here
- **frontend** is the dining room — what customers see and interact with
- **backend** is the kitchen — where the real work happens
- **PostgreSQL** is the pantry — where all data is stored permanently
- **Redis** is the countertop — quick access to frequently used data
- **Ollama** is the specialist cook — handles AI tasks

---

## 3. Backend — Java & Spring Boot

### What is Java?
Java is a programming language that has been around since 1995. It is known for being fast, reliable, and used by large companies. We use **Java 21**, which is the latest long-term support version as of 2024.

### What is Spring Boot?
Spring Boot is a framework built on top of Java. Think of a framework as a pre-built structure — instead of building a house from bricks, you get pre-built walls. Spring Boot gives you:
- A built-in web server (Tomcat) — you don't need to install Apache separately
- Automatic configuration — most things work out of the box
- A huge ecosystem of add-ons (called "starters") for databases, security, caching, email, etc.

We use **Spring Boot 3.3.4**.

### Why Spring Boot?
For a project with this many moving parts (database, AI, scraping, notifications, file upload, security), Spring Boot is the most mature and well-documented choice in Java. Alternatives like Micronaut or Quarkus exist but have smaller communities.

### How is it structured in this project?
The backend is organized by **feature modules**, not by technical layers. This is called "feature-first" structure:

```
module/
├── auth/        Everything about login/register
├── resume/      Everything about resume upload and parsing
├── profile/     Everything about candidate preferences
├── discovery/   Everything about finding jobs
├── matching/    Everything about comparing jobs to your profile
├── generator/   Everything about writing resumes and cover letters
├── notification/ Everything about sending alerts
└── tracker/     Everything about tracking applications
```

Each module has:
- `model/` — the data structure (what does a Job look like?)
- `repository/` — the database queries (how do we find jobs?)
- `service/` — the business logic (how do we decide which jobs to keep?)
- `controller/` — the API endpoint (how does the frontend ask for jobs?)

### What is `pom.xml`?
This is the **shopping list** for Java dependencies (libraries). When you run `mvn install`, Maven reads this file and downloads all listed libraries. It is the Java equivalent of `package.json` in Node.js.

### What is `application.yml`?
This is the **settings file** for the application. Database URL, Redis password, JWT secret, Ollama model name, email server address — all configuration lives here. It is read at startup. You never hardcode secrets in Java code — they go in this file, and the actual values come from environment variables (`.env` file).

### Dev vs Prod profiles
Spring Boot supports profiles — different settings for different environments:
- `application-dev.yml` — used on your laptop. Flyway is off, SQL is logged, job scanning is off
- `application-prod.yml` — used on the server. Flyway is on, scanning is on, logs are minimal

You switch profiles with `SPRING_PROFILES_ACTIVE=prod`.

---

## 4. Database — PostgreSQL, JPA, Flyway

### What is PostgreSQL?
PostgreSQL (often called "Postgres") is a database — software that stores data permanently. When your server restarts, your data is still there. It is one of the most powerful open-source databases available and supports:
- Standard SQL tables and queries
- **JSONB** — storing flexible JSON data inside a table column (used for resume parsed data)
- UUID primary keys (random unique IDs instead of 1, 2, 3...)

We use **PostgreSQL 16**.

### What is a UUID?
UUID stands for Universally Unique Identifier. It looks like: `f47ac10b-58cc-4372-a567-0e02b2c3d479`. Every row in every table has one as its ID. This is better than auto-incrementing numbers (1, 2, 3) because:
- Two different databases can merge data without ID collisions
- IDs are not guessable (security benefit)
- You can generate an ID before saving to the database

### What is JPA / Hibernate?
**JPA** (Java Persistence API) lets you work with database tables as if they were Java objects. Instead of writing SQL like:
```sql
SELECT * FROM jobs WHERE id = '...'
```
You write Java like:
```java
jobRepository.findById(id)
```

**Hibernate** is the actual library that implements JPA. It translates your Java code into SQL behind the scenes.

A JPA **Entity** is a Java class that maps to a database table. For example:
```java
@Entity  // This class maps to a table
@Table(name = "jobs")
public class Job extends BaseEntity {
    private String title;     // maps to "title" column
    private String company;   // maps to "company" column
}
```

### What is BaseEntity?
Every entity in this project extends `BaseEntity`. This parent class automatically provides:
- `id` — UUID primary key, auto-generated
- `createdAt` — timestamp set when the record is first saved
- `updatedAt` — timestamp updated every time the record changes

This means you never have to write these three fields yourself in any entity.

### What is Spring Data JPA?
Spring Data JPA lets you create database query methods by just writing their names:
```java
// Spring generates the SQL for this automatically
List<Job> findBySourceAndActiveTrue(String source);
```
No SQL needed. Spring reads the method name and builds the query.

### What is Flyway?
Flyway is a **database migration tool**. The problem it solves: when your application changes and needs new tables or columns, how do you update the database on the server safely?

Flyway works like this:
- You write numbered SQL files: `V1__init_schema.sql`, `V2__add_index.sql`, etc.
- On startup, Flyway checks which files have already been run and only runs the new ones
- This means the database is always in sync with the code

In this project, Flyway is **disabled in development** (the database is created/updated automatically by Hibernate) and **enabled in production** (where we want controlled, safe migrations).

### JSONB — what and why?
Some data in this project is flexible — the "parsed data" from a resume could contain different fields for different people. Instead of creating 20 separate columns, we store it as JSON inside a single `jsonb` column in PostgreSQL. JSONB is a special PostgreSQL type that stores JSON in a binary format that is fast to query.

In the Resume entity:
```java
@JdbcTypeCode(SqlTypes.JSON)
@Column(columnDefinition = "jsonb")
private Map<String, Object> parsedData;
```

---

## 5. Caching — Redis

### What is caching?
Caching is the practice of storing the result of an expensive operation somewhere fast, so the next time you need it you don't have to do the work again.

Example: Your profile data is fetched from PostgreSQL. That takes ~10ms. If you store the result in Redis, the next fetch takes ~1ms.

### What is Redis?
Redis is an **in-memory database** — it stores data in RAM (not disk), which makes it extremely fast. It is commonly used for caching, session storage, and rate limiting.

We use **Redis 7**.

### How is Redis used in this project?
- `RedisConfig.java` sets up a `RedisTemplate` (the Java object used to talk to Redis)
- A `RedisCacheManager` is configured with a 30-minute TTL (Time To Live) — cached data expires after 30 minutes
- Spring's `@Cacheable` annotation can be added to service methods to automatically cache their results

The password for Redis is set via environment variable (`REDIS_PASSWORD` in `.env`).

---

## 6. Security — JWT & Spring Security

### What is authentication vs authorization?
- **Authentication** = "Who are you?" (login)
- **Authorization** = "What are you allowed to do?" (permissions)

### What is Spring Security?
Spring Security is the standard security framework for Spring Boot. It intercepts every HTTP request before it reaches your controller and decides: is this request allowed?

### What is JWT?
**JWT** stands for JSON Web Token. It is a way to prove your identity without the server needing to remember you.

Here is how it works:
1. You log in with email + password
2. The server verifies your password and creates a JWT — a specially encoded string that contains your email and an expiry date, signed with a secret key
3. The JWT is sent back to you (stored in your browser)
4. Every future request you make includes this JWT in the `Authorization: Bearer <token>` header
5. The server checks the JWT signature — if it is valid, it knows who you are without looking up a database

The JWT in this project looks like three base64-encoded parts joined by dots:
```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIn0.abc123
  ↑ header (algorithm)    ↑ payload (your email, expiry)     ↑ signature
```

The **signature** is the critical part. It is created using a secret key only the server knows. If someone tampers with the payload, the signature won't match and the server rejects it.

### Why stateless JWT?
The alternative is sessions — the server remembers every logged-in user in memory or database. JWT is **stateless** — the server does not need to store anything. This makes it easier to scale (any server can handle any request) and simpler to deploy.

### How is JWT implemented?
- `JwtTokenProvider.java` — creates and validates tokens using the **JJWT** library (version 0.12.6)
- `JwtAuthenticationFilter.java` — runs on every request, extracts the Bearer token, validates it, and tells Spring who the user is
- `SecurityConfig.java` — configures which paths are public (`/auth/**`) and which require a valid JWT

### What is BCrypt?
BCrypt is a **password hashing algorithm**. Passwords are never stored as plain text. They are run through BCrypt, which turns `mypassword123` into something like `$2a$12$abc...xyz`. This hash cannot be reversed — to verify a password, you hash it again and compare.

---

## 7. AI Layer — Ollama, LangChain4j, Spring AI

### What is Ollama?
Ollama is software that runs **large language models (LLMs) locally** on your own computer or server. Instead of sending your data to OpenAI's servers (privacy risk + cost), Ollama downloads and runs the models yourself.

We use two models:
- **llama3.2** — a general-purpose chat/generation model. Used for: skill extraction from resume text, generating optimized resumes, generating cover letters
- **nomic-embed-text** — an embedding model. Used for: converting text into a list of numbers (a vector) that represents its meaning, enabling semantic similarity search

### What is an embedding?
An embedding converts text into a list of numbers. For example:
- "Java developer" → [0.23, -0.41, 0.87, ...]
- "Backend engineer" → [0.21, -0.39, 0.85, ...]

These two are semantically similar (similar meaning), so their number lists are close to each other. This is how the system can eventually match a job description to your career goals even when the exact words are different.

### What is LangChain4j?
LangChain4j is a Java library for building applications with AI models. It provides clean abstractions for:
- Sending prompts and getting responses (`OllamaChatModel`)
- Getting embeddings (`OllamaEmbeddingModel`)
- Building prompt templates

In `OllamaConfig.java`:
```java
@Bean
public OllamaChatModel ollamaChatModel() {
    return OllamaChatModel.builder()
        .baseUrl("http://ollama:11434")  // Ollama server address
        .modelName("llama3.2")
        .timeout(Duration.ofSeconds(120))
        .temperature(0.3)               // Lower = more predictable output
        .build();
}
```

The `temperature` setting is important:
- `0.0` — completely deterministic, always the same output
- `1.0` — very creative/random
- `0.3` — slightly creative but mostly consistent (good for skill extraction)

### What is Spring AI?
Spring AI is Anthropic/Spring's own integration for AI models. Both Spring AI and LangChain4j are in this project — Spring AI handles Spring Boot auto-configuration (reading Ollama URL from `application.yml`), while LangChain4j provides the concrete model beans used in services.

### How is AI used for skill extraction?
In `SkillExtractionService.java`:
1. The raw text of your resume is sent to Ollama with a prompt like:
   > "You are a technical recruiter. Extract all technical skills from this resume text. Return ONLY a JSON array of skill names. Resume text: [your resume]"
2. Ollama responds with: `["Java", "Spring Boot", "PostgreSQL", "React", ...]`
3. The service parses this JSON and saves each skill as a `CandidateSkill` record

### How is AI used for cover letters?
In `CoverLetterService.java`:
1. Your skills, experience excerpt, and the job description are assembled into a prompt
2. Ollama generates a 3-4 paragraph cover letter
3. The result is returned to the frontend as a string

---

## 8. Document Parsing — PDFBox & Apache Tika

### The problem
When a user uploads a resume, it is a PDF or DOCX file. We cannot read skills from a binary file — we need the plain text inside it first.

### What is Apache PDFBox?
PDFBox is a Java library specifically for reading PDF files. It can extract all the text from a PDF while preserving reasonable formatting.

```java
PDDocument document = PDDocument.load(inputStream);
PDFTextStripper stripper = new PDFTextStripper();
String text = stripper.getText(document);
```

### What is Apache Tika?
Tika is a more general document parsing library — it can handle PDF, DOCX, Excel, PowerPoint, and many other formats. We use it for DOCX files and as a fallback.

### Why both?
PDFBox is more reliable for PDFs specifically. Tika is better as a catch-all for other document types. The `ResumeParserService` checks the file type and routes to the appropriate parser.

---

## 9. Web Scraping — Playwright

### What is web scraping?
Web scraping means automatically reading data from websites that don't provide an official API. Most job boards don't give you a developer API to list jobs — you have to load the webpage and extract the data from the HTML.

### What is Playwright?
Playwright is a **headless browser** tool made by Microsoft. "Headless" means the browser runs without a visible window — it works silently in the background.

Playwright can:
- Open a URL in a real Chrome browser (without displaying it)
- Wait for JavaScript to load (important — many job sites load data dynamically with JavaScript)
- Find elements on the page by CSS selector
- Extract text from those elements

### How is it used?
In `NaukriScraper.java`:
```java
try (Playwright playwright = Playwright.create()) {
    Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    Page page = browser.newPage();
    page.navigate("https://www.naukri.com/jobs?keyword=Java+Developer&location=Bangalore");
    // Wait for job cards to appear
    page.waitForSelector(".jobTuple");
    // Extract data from each card
    for (ElementHandle card : page.querySelectorAll(".jobTuple")) {
        String title = card.querySelector(".title").innerText();
        // ...
    }
}
```

The CSS selectors (`.jobTuple`, `.title`) are placeholders in the scaffold — they need to be updated by inspecting the actual Naukri HTML.

### Why Playwright over simpler HTTP requests?
Simple HTTP libraries (like `HttpClient`) cannot run JavaScript. Many modern job sites (Naukri, LinkedIn, etc.) load their job listings dynamically using JavaScript after the initial page load. Playwright loads the full page like a real browser, so JavaScript runs and data appears.

---

## 10. Notifications — Telegram & Email

### Telegram notifications
**Why Telegram?** It is free, has an excellent Bot API, and delivers messages instantly to your phone.

**How it works:**
1. You create a bot on Telegram via BotFather (a special Telegram account)
2. BotFather gives you a bot token (like a password for your bot)
3. When someone messages your bot `/start`, the bot receives their chat ID
4. The server uses this chat ID to send them messages

In `TelegramBot.java`:
- The class extends `TelegramLongPollingBot` — this means it constantly asks Telegram "any new messages?" (long polling)
- When a user sends `/start`, the bot replies with their chat ID
- The user copies that chat ID into their profile settings
- From then on, the server can push messages to that user

### Email notifications
**How it works:** Spring's `JavaMailSender` connects to your SMTP server (Gmail, SendGrid, etc.) and sends emails. Configuration is in `application.yml` under `spring.mail`.

### Why async (`@Async`)?
Sending a notification takes time — the server must make an HTTP request to Telegram or connect to an SMTP server. We don't want the main job-scanning thread to wait for this. `@Async` means the notification is sent in a background thread so the scanner can continue immediately.

---

## 11. Frontend — React, TypeScript, Vite

### What is React?
React is a JavaScript library for building user interfaces. The core idea is **components** — small, reusable pieces of UI. Instead of writing one giant HTML file, you write small components (`JobCard`, `Badge`, `Header`) and compose them together.

We use **React 18** (the latest major version).

### What is TypeScript?
TypeScript is JavaScript with **types**. Types let you define what shape your data has, and TypeScript tells you at development time (before running) if you make a mistake.

Without TypeScript:
```js
const job = fetchJob()
job.titlee  // typo — no error until runtime
```

With TypeScript:
```ts
const job: Job = fetchJob()
job.titlee  // ERROR: Property 'titlee' does not exist on type 'Job'
```

TypeScript catches bugs before they reach users.

### What is Vite?
Vite is a **build tool** for frontend projects. It does two things:
1. **Development server** — serves your React app with instant hot reload (change code → browser updates in <100ms)
2. **Production build** — bundles all your JavaScript/TypeScript/CSS into optimized files for deployment

Vite replaced older tools like Webpack and Create React App because it is dramatically faster.

### The `frontend/src/` structure
```
api/          Functions that make HTTP calls to the backend
types/        TypeScript interfaces (Job, Profile, etc.)
store/        Zustand stores (auth state)
viewmodels/   Logic hooks (MVVM pattern)
pages/        One folder per route/screen
components/   Reusable UI pieces
```

### Environment variables in Vite
In Vite, environment variables must start with `VITE_` to be accessible in browser code:
- `VITE_API_BASE_URL=http://localhost:8080` — the backend URL
- This is read in `src/api/client.ts` via `import.meta.env.VITE_API_BASE_URL`

---

## 12. Styling — Tailwind CSS

### What is Tailwind CSS?
Tailwind is a CSS framework where instead of writing CSS in separate files, you apply **utility classes** directly in your HTML/JSX.

Traditional CSS:
```css
/* styles.css */
.card {
    background: white;
    border-radius: 8px;
    padding: 16px;
    box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}
```
```html
<div class="card">...</div>
```

Tailwind:
```html
<div class="bg-white rounded-lg p-4 shadow-sm">...</div>
```

### Why Tailwind?
- No switching between files — styles are co-located with the component
- No naming things — no need to invent class names like `.card-container-wrapper`
- Consistent spacing/colors — uses a design system by default (p-4 = 16px, p-8 = 32px, etc.)
- Easy responsive design — `lg:grid-cols-4` means "4 columns on large screens"

### Custom brand colors
In `tailwind.config.js`, custom `brand` colors are defined:
```js
colors: {
    brand: {
        50: '#f0f9ff',
        600: '#0284c7',
        700: '#0369a1',
    }
}
```
This allows classes like `text-brand-600`, `bg-brand-50` throughout the app.

### Reusable component classes
In `src/index.css`, common patterns are extracted into named classes using `@layer components`:
```css
.card    → white background, rounded corners, shadow, padding
.btn-primary → brand-colored button
.input   → styled form input
.label   → styled form label
```

These are used everywhere instead of repeating 10 Tailwind classes.

---

## 13. Frontend Data Fetching — Tanstack Query

### The problem with plain fetch/axios
When you fetch data in a React component, you have to manually manage:
- Loading state (`isLoading = true` while fetching)
- Error state (what if the request fails?)
- Caching (should we refetch when the user comes back to this page?)
- Stale data (is this data still fresh?)
- Background refetching (update silently when data changes)

Doing all this manually for every API call results in a lot of repetitive, bug-prone code.

### What is Tanstack Query?
Tanstack Query (formerly React Query) manages **server state** — data that lives on the server and needs to be fetched, cached, and synchronized.

```ts
const { data, isLoading, error } = useQuery({
    queryKey: ['jobs'],              // unique key for this data
    queryFn: () => jobsApi.list(),   // how to fetch it
})
```

With this one hook, you automatically get:
- `isLoading: true` on first fetch
- `data` populated when fetch completes
- Automatic caching — if you navigate away and come back, shows cached data instantly, then refetches in background
- `error` if the request fails
- Automatic refetch if the window regains focus

### useMutation — for writes
`useMutation` handles POST/PATCH/DELETE requests:
```ts
const updateStatus = useMutation({
    mutationFn: ({ id, status }) => trackerApi.updateStatus(id, status),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['applications'] }),
})
```

`invalidateQueries` tells Tanstack Query: "the data for ['applications'] is now stale — refetch it." This is how the UI updates automatically after a mutation.

### Query Keys
Every query has a key — an array used to identify and cache the data:
- `['jobs']` — all jobs
- `['profile']` — current user's profile
- `['applications']` — current user's applications
- `['tracker-stats']` — dashboard counts

When you call `invalidateQueries({ queryKey: ['applications'] })`, every query with that key is marked stale and refetched.

---

## 14. Frontend State — Zustand

### Two kinds of state
1. **Server state** — data that comes from the API (jobs, profile, applications). Managed by Tanstack Query.
2. **Client state** — data that lives only in the browser (is the user logged in? what is their name?). Managed by Zustand.

### What is Zustand?
Zustand is a minimalist state management library. It creates a **store** — a shared data container that any component can read from or write to.

The auth store in `src/store/authStore.ts`:
```ts
export const useAuthStore = create(persist(
    (set) => ({
        token: null,
        isAuthenticated: false,
        login: (token, userId, name, email) => set({ token, userId, name, email, isAuthenticated: true }),
        logout: () => set({ token: null, isAuthenticated: false }),
    }),
    { name: 'auth-store' }  // persist to localStorage
))
```

### Why `persist`?
Without `persist`, the state is lost when you refresh the page. With `persist`, Zustand automatically saves the store to `localStorage` under the key `'auth-store'`. When you reload the page, Zustand reads localStorage and restores the state — so you stay logged in.

### Using the store in a component
```ts
const { isAuthenticated, name } = useAuthStore()
const logout = useAuthStore((s) => s.logout)
```

Any component that reads from the store will automatically re-render when the store changes.

---

## 15. Frontend Routing — React Router

### What is client-side routing?
In a traditional website, navigating to `/jobs` makes the browser request a new page from the server. In a **Single Page Application (SPA)** like this React app, there is only one HTML file. React Router intercepts URL changes and swaps the displayed component without making a server request. This makes navigation feel instant.

### How routing is configured
In `src/App.tsx`:
```tsx
<BrowserRouter>
    <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route element={<ProtectedRoute />}>   {/* requires login */}
            <Route path="/" element={<Dashboard />} />
            <Route path="/jobs" element={<Jobs />} />
            {/* ... */}
        </Route>
    </Routes>
</BrowserRouter>
```

### What is ProtectedRoute?
A component that checks `isAuthenticated` from Zustand. If the user is not logged in, it redirects them to `/login`. If they are logged in, it renders the page normally. This protects every page behind that route.

### What is `<Outlet>`?
The `Layout` component contains `<Outlet />`. This is a placeholder — React Router fills it with whatever child route matches. So `Layout` renders the sidebar and header, and `<Outlet />` shows the current page content.

---

## 16. HTTP Client — Axios

### What is Axios?
Axios is a JavaScript library for making HTTP requests (API calls). It is a popular alternative to the browser's built-in `fetch`. The advantage is cleaner syntax and **interceptors**.

### The base client
In `src/api/client.ts`:
```ts
const apiClient = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL
})
```

This creates an axios instance where all requests automatically start with the backend URL. So `apiClient.get('/jobs')` becomes `GET http://localhost:8080/jobs`.

### Request interceptor
An interceptor runs before every request:
```ts
apiClient.interceptors.request.use((config) => {
    const token = useAuthStore.getState().token
    if (token) {
        config.headers.Authorization = `Bearer ${token}`
    }
    return config
})
```
This automatically adds the JWT token to every request — you never have to add it manually in each API function.

### Response interceptor
This runs after every response:
```ts
apiClient.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            useAuthStore.getState().logout()
            window.location.href = '/login'
        }
        return Promise.reject(error)
    }
)
```
If the server returns 401 (token expired or invalid), the user is automatically logged out and sent to the login page.

---

## 17. Architecture Pattern — MVVM

### What is MVVM?
MVVM stands for **Model-View-ViewModel**. It is a pattern for organizing code so that UI (View) is separated from logic (ViewModel) and from data shapes (Model).

### Why does this matter?
Without MVVM, all code ends up in the page component — data fetching, business logic, and rendering mixed together. This makes components:
- Hard to test (you can't test logic without rendering)
- Hard to reuse (the logic is stuck in one component)
- Hard to read (you have to mentally separate concerns)

### The three layers in this project

**Model** = `src/types/index.ts`
TypeScript interfaces that define what your data looks like:
```ts
interface Job {
    id: string
    title: string
    company: string
    requiredSkills: string[]
    // ...
}
```

**ViewModel** = `src/viewmodels/use*ViewModel.ts`
Custom hooks that own all the logic:
```ts
export function useJobsViewModel() {
    // fetches jobs from API
    // tracks which jobs are saved
    // exposes saveJob() action
    return { jobs, savedJobIds, saveJob, isLoading }
}
```

**View** = `src/pages/*`
React components that only do rendering:
```tsx
export default function Jobs() {
    const { jobs, savedJobIds, saveJob } = useJobsViewModel()
    // only renders — no logic here
    return <div>{jobs.map(job => <JobCard ... />)}</div>
}
```

### The rule
> Pages import exactly one ViewModel. ViewModels import from api and types. Nothing else crosses layers.

---

## 18. Infrastructure — Docker & docker-compose

### What is Docker?
Docker is a tool for running applications in **containers**. A container is like a lightweight virtual machine — it packages your application and all its dependencies together so it runs the same way on any machine.

Without Docker, deploying your app to a server means:
1. Install Java 21 on the server
2. Install Node.js on the server
3. Install PostgreSQL on the server
4. Install Redis on the server
5. Install nginx on the server
6. Configure everything to talk to each other
7. Hope nothing conflicts with other software on the server

With Docker, you just run `docker-compose up`. Docker downloads and starts everything in isolated containers.

### What is a Dockerfile?
A Dockerfile is a recipe for building a Docker image (a snapshot of an application). 

The backend Dockerfile uses a **multi-stage build**:
```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
COPY pom.xml .
RUN mvn dependency:go-offline  # download dependencies (cached separately)
COPY src/ src/
RUN mvn package -DskipTests    # compile and package

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine  # much smaller image — no Maven
COPY --from=builder target/app.jar app.jar
RUN adduser spring  # security: don't run as root
USER spring
CMD ["java", "-jar", "app.jar"]
```

Why multi-stage? The final image only contains the JRE (Java Runtime) and the JAR file — not Maven, not source code. This makes the image much smaller (~200MB vs ~600MB).

### What is docker-compose?
docker-compose is a tool for running **multiple containers together**. Our `docker-compose.yml` defines 6 services and how they connect:
```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: aicareer_db
  
  backend:
    build: ./backend
    depends_on:
      postgres:
        condition: service_healthy  # wait for postgres to be ready
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/aicareer_db
```

Notice `postgres` in the URL — inside Docker, containers talk to each other by service name, not by `localhost`.

### Health checks
The postgres and redis services have health checks:
```yaml
healthcheck:
  test: ["CMD-SHELL", "pg_isready -U postgres"]
  interval: 10s
  retries: 5
```

This means Docker waits until PostgreSQL is actually accepting connections before starting the backend — preventing startup failures.

### Volumes
```yaml
volumes:
  postgres_data:    # PostgreSQL data survives container restarts
  redis_data:       # Redis data survives container restarts
  ollama_data:      # Downloaded AI models survive container restarts
  resume_uploads:   # Uploaded resume files are stored here
```

Without volumes, all data disappears when containers are stopped.

---

## 19. Reverse Proxy — nginx

### What is a reverse proxy?
A reverse proxy sits in front of your servers and routes incoming requests to the right place. Instead of exposing your backend on port 8080 and your frontend on port 3000, you expose only nginx on port 80, and nginx forwards requests internally.

```
Browser → nginx:80 → decides where to send:
    /api/jobs       →  backend:8080/jobs
    /api/auth/login →  backend:8080/auth/login
    /               →  frontend:80 (React app)
    /jobs           →  frontend:80 (React app handles routing)
```

### Why strip `/api` prefix?
The browser calls `/api/jobs`. nginx strips `/api` and calls the backend with just `/jobs`. This way the backend doesn't know it's behind a proxy — its routes are clean (`/jobs`, not `/api/jobs`).

### SPA routing
For a React SPA, any URL (`/jobs`, `/profile`, `/tracker`) must return the same `index.html` file. React Router then handles the URL client-side. In `nginx-spa.conf`:
```nginx
location / {
    try_files $uri $uri/ /index.html;
}
```
This says: "try the exact file, then a directory, then fall back to index.html". This is the standard nginx config for SPAs.

---

## 20. CI/CD — GitHub Actions

### What is CI/CD?
- **CI (Continuous Integration)** — automatically run tests and build checks whenever you push code
- **CD (Continuous Deployment)** — automatically deploy to production when tests pass

### What does the GitHub Actions workflow do?
The `.github/workflows/ci.yml` file defines three jobs that run automatically on every push to GitHub:

1. **Backend job** — starts a PostgreSQL service container, runs `mvn verify` (compiles + tests)
2. **Frontend job** — runs `npm ci` (install dependencies), `tsc --noEmit` (TypeScript type check), `npm run build` (production build)
3. **Docker job** — builds both Docker images to verify they compile correctly (only on the `main` branch)

### Why GitHub Actions?
It is free for public repos and cheap for private repos. It runs in the cloud so you don't need your own build server. The workflow file lives in the repository, so build configuration is version-controlled alongside code.

---

## 21. The 11 Modules — What Each One Does

### Module 1: Resume Intelligence Engine
**What it does:** Takes your uploaded PDF/DOCX resume, extracts all text from it, then uses AI to identify your skills.

**The chain:**
1. `ResumeController` receives the uploaded file
2. `ResumeService.upload()` saves the file to disk (in `{uploadDir}/{userId}/`) and creates a database record
3. `ResumeParserService.extractText()` reads the file and returns plain text
4. `SkillExtractionService.extractSkillsAsJson()` sends the text to Ollama and gets back a JSON array of skills
5. Skills are saved as `CandidateSkill` records with `inferred=true, confirmed=false`

**Why separate parse and extract?**
Parsing (PDF → text) is fast and deterministic. AI extraction is slow (seconds) and probabilistic. Separating them lets you re-run AI extraction without re-uploading the file.

---

### Module 2 & 3: Candidate Profile Builder & Master Profile
**What it does:** Stores everything about you — your preferences (roles, locations, work mode), career goals, and your confirmed skill list.

**Key concept:** The `CandidateProfile` is the central record used for job matching. It has:
- `preferredRoles` — ["Backend Developer", "Java Engineer"]
- `preferredLocations` — ["Bangalore", "Remote"]
- `careerGoals` — ["Lead a team", "Work on distributed systems"]
- `skills` — list of `CandidateSkill` with proficiency levels

**Why separate "inferred" from "confirmed" skills?**
AI is not perfect. If your resume mentions "supervised a team of 5" it might infer "Management" as a skill. You should confirm this before it affects your job matches. Skills stay `inferred=true` until you click "Confirm" in the UI.

---

### Module 4: Real-Time Job Discovery Engine
**What it does:** Automatically scrapes job boards and saves new job listings to your database.

**Key concepts:**
- `JobScraper` is an **interface** — a contract saying "you must implement `scrape(query, location)`"
- `NaukriScraper` is one implementation of `JobScraper`
- `JobDiscoveryService` gets all scrapers automatically injected (Spring finds all beans implementing `JobScraper`) and runs them all
- New jobs are deduplicated by `(source, externalJobId)` — a job already in your database won't be saved again

**Why the interface pattern?**
Adding a new job board (LinkedIn, Foundit, etc.) just means creating a new class that implements `JobScraper`. No existing code changes. This is the **Open/Closed Principle** — open for extension, closed for modification.

---

### Module 5: Freshness Detection Engine
**What it does:** Scores how recently a job was posted, from 0 (very old) to 100 (just posted).

**Formula:** Linear decay over 7 days (168 hours). A job posted 1 hour ago scores ~99. A job posted 3 days ago scores ~57. A job posted 7+ days ago scores 0.

**Why freshness matters:** Applying to a 3-week-old job listing often means the role is already filled. The freshness score helps prioritize truly fresh opportunities.

---

### Module 6: AI Job Matching Engine
**What it does:** Compares a job's required skills against your skills and produces a match score.

**The MatchResult has:**
- `matchScore` — percentage of required skills you have
- `freshnessScore` — from FreshnessService
- `goalAlignmentScore` — how well the job aligns with your career goals (currently a placeholder at 70.0 — needs embedding similarity implementation)
- `priorityScore` — weighted average: `(matchScore × 0.5) + (freshnessScore × 0.3) + (goalAlignmentScore × 0.2)`
- `priority` label — CRITICAL (≥85), HIGH (≥70), MEDIUM (≥50), LOW

---

### Module 7: Job Priority Engine
**What it does:** Assigns a human-readable priority label (CRITICAL / HIGH / MEDIUM / LOW) to each match result based on the weighted priority score.

This is part of `MatchResult` — the `toPriorityLabel(double score)` static method. It is built into the matching module.

---

### Module 8: Resume Optimization Engine
**What it does:** Takes your existing resume text and a job description, sends both to Ollama, and gets back a rewritten resume tailored for that specific job (ATS-optimized).

**ATS** (Applicant Tracking System) is software companies use to automatically filter resumes. Keywords from the job description must appear in your resume. The AI rewrite ensures this.

---

### Module 9: Cover Letter Generator
**What it does:** Generates a professional cover letter by combining your name, skills, experience excerpt, and the job description into a prompt for Ollama.

The prompt specifies tone (professional + enthusiastic) and structure (3-4 paragraphs: opening, skill-job connection, enthusiasm, closing).

---

### Module 10: Notification Service
**What it does:** Sends you a Telegram message and/or email when a CRITICAL or HIGH priority job is found.

**Message includes:** Job title, company, location, priority label, match percentage, matched skills, missing skills.

**Why async?** Notifications are sent in background threads (`@Async`) so they don't block the job scanning loop.

---

### Module 11: Application Tracker
**What it does:** Tracks which jobs you have saved, applied to, interviewed for, rejected, or received offers from.

**The `JobApplication` entity** links a `User` to a `Job` with a status:
`SAVED → APPLIED → INTERVIEW → OFFER/REJECTED`

**Dashboard stats** are computed with a `@Query` that counts applications by status — shown as the 4 stat cards on the Dashboard page.

---

## 22. How Everything Connects (end-to-end flow)

Here is the complete journey from "new user" to "receiving a job notification":

```
1. Register
   Browser → POST /auth/register → AuthService.register()
   → User saved to PostgreSQL
   → JWT token returned → stored in browser localStorage (Zustand)

2. Upload Resume
   Browser → POST /resumes/upload (multipart) → ResumeService.upload()
   → File saved to disk (./uploads/{userId}/)
   → Raw text extracted (PDFBox/Tika)
   → Resume record saved to PostgreSQL

3. Extract Skills
   Browser → POST /resumes/{id}/extract-skills → ResumeService.extractAndSaveSkills()
   → Text sent to Ollama (llama3.2)
   → Skills parsed from JSON response
   → CandidateSkill records saved (inferred=true, confirmed=false)

4. Confirm Skills + Set Preferences
   Browser → POST /profile/skills/confirm → ProfileService.confirmSkills()
   → Skills marked confirmed=true
   Browser → PUT /profile/preferences → ProfileService.updatePreferences()
   → Preferred roles, locations, career goals saved

5. Job Scanner runs (background, every N minutes)
   JobScanScheduler.scanJobs()
   → Finds all CandidateProfiles that are complete
   → For each profile, for each (role, location) pair:
     → NaukriScraper.scrape("Java Developer", "Bangalore")
       → Playwright opens Naukri in headless Chrome
       → Extracts job cards
       → Returns List<Job>
     → New jobs saved (duplicates skipped)
   → FreshnessService.refreshFreshnessScores() updates all jobs

6. Matching (triggered after scan)
   For each new job × each profile:
   → JobMatchingService.match(profile, job)
   → MatchResult calculated with priority score

7. Notification (for HIGH/CRITICAL matches)
   NotificationService.notifyHighPriorityJob(user, matchResult)  [async]
   → If user.telegramNotificationsEnabled → TelegramBot.sendMessage(chatId, text)
   → If user.emailNotificationsEnabled → JavaMailSender.send(email, text)

8. User browses results
   Browser → GET /jobs → JobController → paginated list of discovered jobs
   User clicks "Save" → POST /tracker/save/{jobId} → JobApplication (SAVED)
   User clicks "Apply" → POST /tracker/apply/{jobId} → JobApplication (APPLIED)
   User views → POST /generate/cover-letter/{jobId} → CoverLetterService
   → Ollama generates cover letter → returned to browser
```

---

## Quick Glossary

| Term | Plain English |
|------|--------------|
| API | A set of URLs your app calls to get/send data |
| REST | A style of API using standard HTTP methods (GET, POST, PATCH, DELETE) |
| JSON | A text format for data: `{"name": "John", "age": 30}` |
| JWT | A tamper-proof string that proves who you are |
| ORM | Software that lets you use database tables as code objects |
| Migration | A versioned SQL script that safely updates the database schema |
| Cache | Temporary fast storage for frequently-used data |
| Headless browser | A browser that runs without a visible window |
| Embedding | A list of numbers representing the meaning of text |
| LLM | Large Language Model — AI that understands and generates text |
| Bean | In Spring, any object managed by the framework (created, configured, injected automatically) |
| Dependency Injection | Spring automatically provides objects to classes that need them |
| SPA | Single Page Application — one HTML file, JavaScript handles navigation |
| Hot reload | Development server updates the browser instantly when you save a file |
| Hydration | React attaching event listeners to server-rendered HTML |
| Query key | Tanstack Query's identifier for a specific piece of server data |
| Stateless | The server keeps no memory between requests — each request is self-contained |
