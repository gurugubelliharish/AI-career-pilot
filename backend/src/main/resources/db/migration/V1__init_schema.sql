-- ─── Users ───────────────────────────────────────────────────────────────────
CREATE TABLE users (
    id                              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                            VARCHAR(255) NOT NULL,
    email                           VARCHAR(255) UNIQUE NOT NULL,
    password                        VARCHAR(255) NOT NULL,
    role                            VARCHAR(50)  DEFAULT 'USER',
    telegram_chat_id                VARCHAR(100),
    email_notifications_enabled     BOOLEAN DEFAULT TRUE,
    telegram_notifications_enabled  BOOLEAN DEFAULT FALSE,
    active                          BOOLEAN DEFAULT TRUE,
    created_at                      TIMESTAMPTZ DEFAULT NOW(),
    updated_at                      TIMESTAMPTZ DEFAULT NOW()
);

-- ─── Resumes ─────────────────────────────────────────────────────────────────
CREATE TABLE resumes (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    file_name    VARCHAR(255) NOT NULL,
    file_path    VARCHAR(500) NOT NULL,
    file_type    VARCHAR(50),
    raw_text     TEXT,
    parsed_data  JSONB,
    processed    BOOLEAN DEFAULT FALSE,
    active       BOOLEAN DEFAULT TRUE,
    created_at   TIMESTAMPTZ DEFAULT NOW(),
    updated_at   TIMESTAMPTZ DEFAULT NOW()
);

-- ─── Candidate Profiles ──────────────────────────────────────────────────────
CREATE TABLE candidate_profiles (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name                VARCHAR(255),
    email               VARCHAR(255),
    phone               VARCHAR(50),
    linkedin_url        VARCHAR(500),
    github_url          VARCHAR(500),
    preferred_roles     TEXT[],
    preferred_locations TEXT[],
    work_mode           VARCHAR(50),
    experience_level    VARCHAR(50),
    expected_salary     VARCHAR(100),
    notice_period       VARCHAR(100),
    career_goals        TEXT[],
    education           JSONB,
    experience          JSONB,
    projects            JSONB,
    certifications      JSONB,
    profile_complete    BOOLEAN DEFAULT FALSE,
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    updated_at          TIMESTAMPTZ DEFAULT NOW()
);

-- ─── Candidate Skills ────────────────────────────────────────────────────────
CREATE TABLE candidate_skills (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id  UUID NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
    skill_name  VARCHAR(255) NOT NULL,
    proficiency VARCHAR(50),   -- BEGINNER | INTERMEDIATE | ADVANCED
    inferred    BOOLEAN DEFAULT FALSE,
    confirmed   BOOLEAN DEFAULT FALSE,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (profile_id, skill_name)
);

-- ─── Jobs ────────────────────────────────────────────────────────────────────
CREATE TABLE jobs (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_job_id  VARCHAR(500),
    title            VARCHAR(500) NOT NULL,
    company          VARCHAR(255) NOT NULL,
    location         VARCHAR(255),
    description      TEXT,
    work_mode        VARCHAR(50),
    experience_level VARCHAR(100),
    salary_range     VARCHAR(100),
    source           VARCHAR(100) NOT NULL,
    source_url       VARCHAR(1000),
    required_skills  TEXT[],
    posted_at        TIMESTAMPTZ,
    freshness_score  DOUBLE PRECISION DEFAULT 0.0,
    active           BOOLEAN DEFAULT TRUE,
    created_at       TIMESTAMPTZ DEFAULT NOW(),
    updated_at       TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (source, external_job_id)
);

-- ─── Job Applications ────────────────────────────────────────────────────────
CREATE TABLE job_applications (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id               UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    job_id                UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    status                VARCHAR(50) DEFAULT 'SAVED',   -- SAVED | APPLIED | INTERVIEW | REJECTED | OFFER
    match_score           DOUBLE PRECISION DEFAULT 0.0,
    priority              VARCHAR(50),                   -- CRITICAL | HIGH | MEDIUM | LOW
    notes                 TEXT,
    tailored_resume_path  VARCHAR(500),
    cover_letter_path     VARCHAR(500),
    applied_at            TIMESTAMPTZ,
    created_at            TIMESTAMPTZ DEFAULT NOW(),
    updated_at            TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (user_id, job_id)
);

-- ─── Notifications ───────────────────────────────────────────────────────────
CREATE TABLE notifications (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type       VARCHAR(50) NOT NULL,    -- JOB_MATCH | APPLICATION_UPDATE
    channel    VARCHAR(50) NOT NULL,    -- TELEGRAM | EMAIL
    content    TEXT NOT NULL,
    sent       BOOLEAN DEFAULT FALSE,
    sent_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ─── Indexes ─────────────────────────────────────────────────────────────────
CREATE INDEX idx_resumes_user_id           ON resumes(user_id);
CREATE INDEX idx_candidate_profiles_user   ON candidate_profiles(user_id);
CREATE INDEX idx_candidate_skills_profile  ON candidate_skills(profile_id);
CREATE INDEX idx_jobs_source               ON jobs(source);
CREATE INDEX idx_jobs_posted_at            ON jobs(posted_at DESC);
CREATE INDEX idx_jobs_active               ON jobs(active);
CREATE INDEX idx_applications_user_id      ON job_applications(user_id);
CREATE INDEX idx_applications_status       ON job_applications(status);
CREATE INDEX idx_notifications_user_id     ON notifications(user_id);
CREATE INDEX idx_notifications_sent        ON notifications(sent);
