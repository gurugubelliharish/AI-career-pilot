-- JPA @ElementCollection tables that were missing from V1
-- V1 used native PostgreSQL TEXT[] columns; entities use @ElementCollection junction tables.

-- ── Drop obsolete array columns ───────────────────────────────────────────────
ALTER TABLE jobs               DROP COLUMN IF EXISTS required_skills;
ALTER TABLE candidate_profiles DROP COLUMN IF EXISTS preferred_roles;
ALTER TABLE candidate_profiles DROP COLUMN IF EXISTS preferred_locations;
ALTER TABLE candidate_profiles DROP COLUMN IF EXISTS career_goals;

-- ── Job required skills ───────────────────────────────────────────────────────
CREATE TABLE job_required_skills (
    job_id UUID        NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    skill  VARCHAR(255)
);
CREATE INDEX idx_job_required_skills_job_id ON job_required_skills(job_id);

-- ── Profile preferred roles ───────────────────────────────────────────────────
CREATE TABLE profile_preferred_roles (
    profile_id UUID        NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
    role       VARCHAR(255)
);
CREATE INDEX idx_profile_preferred_roles ON profile_preferred_roles(profile_id);

-- ── Profile preferred locations ───────────────────────────────────────────────
CREATE TABLE profile_preferred_locations (
    profile_id UUID        NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
    location   VARCHAR(255)
);
CREATE INDEX idx_profile_preferred_locations ON profile_preferred_locations(profile_id);

-- ── Profile career goals ──────────────────────────────────────────────────────
CREATE TABLE profile_career_goals (
    profile_id UUID NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
    goal       TEXT
);
CREATE INDEX idx_profile_career_goals ON profile_career_goals(profile_id);
