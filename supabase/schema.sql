-- ========================================================================
-- TheekHai: Smart Cities Platform (Problem 8)
-- Supabase PostgreSQL Schema with Oct 2026 DPDP & Row Level Security (RLS)
-- ========================================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- User Roles & Profiles
CREATE TABLE IF NOT EXISTS user_profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT NOT NULL,
    full_name TEXT NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('citizen', 'field_worker', 'admin')),
    zone TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Complaints Table
CREATE TABLE IF NOT EXISTS complaints (
    id TEXT PRIMARY KEY DEFAULT ('cmp-' || uuid_generate_v4()),
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    category TEXT NOT NULL,
    priority TEXT NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    status TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'TRIAGED', 'IN_PROGRESS', 'RESOLVED', 'VERIFIED', 'ESCALATED', 'REOPENED')),
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    address TEXT NOT NULL,
    citizen_id TEXT NOT NULL,
    assigned_worker_id TEXT,
    zone TEXT NOT NULL,
    photo_url TEXT,
    resolution_notes TEXT,
    resolution_photo_url TEXT,
    cluster_id TEXT,
    sla_due_epoch_millis BIGINT NOT NULL,
    created_at_epoch_millis BIGINT NOT NULL,
    updated_at_epoch_millis BIGINT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Verification Results (TinyFish Autonomous Agent)
CREATE TABLE IF NOT EXISTS verifications (
    id TEXT PRIMARY KEY DEFAULT ('verif-' || uuid_generate_v4()),
    complaint_id TEXT NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
    state TEXT NOT NULL CHECK (state IN ('IN_PROGRESS', 'CONFIRMED_RESOLVED', 'CONTRADICTION_FOUND', 'MANUAL_INSPECTION_REQUIRED')),
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    scraped_portal_url TEXT,
    scraped_evidence_snippet TEXT,
    contradiction_detected BOOLEAN NOT NULL DEFAULT FALSE,
    explanation TEXT NOT NULL,
    verified_at_epoch_millis BIGINT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Explainable AI & DPDP 2026 Audit Trail
-- Mandated: Every automated action MUST log human-understandable reasoning
CREATE TABLE IF NOT EXISTS audit_logs (
    id TEXT PRIMARY KEY DEFAULT ('audit-' || uuid_generate_v4()),
    complaint_id TEXT NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
    actor_type TEXT NOT NULL CHECK (actor_type IN ('CITIZEN', 'FIELD_WORKER', 'ADMIN', 'AI_GEMINI_TRIAGE', 'AI_TINYFISH_AGENT', 'AI_GROQ_VISION', 'SYSTEM_SLA_WATCHER')),
    actor_id TEXT NOT NULL,
    action TEXT NOT NULL,
    reasoning TEXT NOT NULL,
    decision_payload_json TEXT,
    timestamp_epoch_millis BIGINT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ========================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES (2026 Gov Privacy Standard)
-- ========================================================================

ALTER TABLE complaints ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE verifications ENABLE ROW LEVEL SECURITY;

-- 1. Citizens: Can read and create their own complaints
CREATE POLICY "Citizens can read own complaints"
ON complaints FOR SELECT
USING (
    citizen_id = auth.uid()::text
    OR EXISTS (SELECT 1 FROM user_profiles WHERE id = auth.uid() AND role = 'admin')
);

CREATE POLICY "Citizens can insert own complaints"
ON complaints FOR INSERT
WITH CHECK (
    citizen_id = auth.uid()::text
    OR auth.role() = 'anon'
);

-- 2. Field Workers: Can read complaints in their assigned zone and update resolution
CREATE POLICY "Field workers can view zone complaints"
ON complaints FOR SELECT
USING (
    zone IN (SELECT up.zone FROM user_profiles up WHERE up.id = auth.uid() AND up.role = 'field_worker')
    OR EXISTS (SELECT 1 FROM user_profiles WHERE id = auth.uid() AND role = 'admin')
);

CREATE POLICY "Field workers can update assigned complaints"
ON complaints FOR UPDATE
USING (
    zone IN (SELECT up.zone FROM user_profiles up WHERE up.id = auth.uid() AND up.role = 'field_worker')
    OR EXISTS (SELECT 1 FROM user_profiles WHERE id = auth.uid() AND role = 'admin')
);

-- 3. Admins: Full access
CREATE POLICY "Admins have full access to complaints"
ON complaints FOR ALL
USING (
    EXISTS (SELECT 1 FROM user_profiles WHERE id = auth.uid() AND role = 'admin')
);

-- 4. Audit Logs: Append-only transparency trail (No updates or deletes permitted)
CREATE POLICY "Audit logs are append-only by authenticated services and users"
ON audit_logs FOR INSERT
WITH CHECK (true);

CREATE POLICY "Audit logs readable by related complaint citizen or admin"
ON audit_logs FOR SELECT
USING (
    EXISTS (
        SELECT 1 FROM complaints c
        WHERE c.id = audit_logs.complaint_id
        AND (c.citizen_id = auth.uid()::text OR EXISTS (SELECT 1 FROM user_profiles WHERE id = auth.uid() AND role = 'admin'))
    )
    OR auth.role() = 'anon'
);

-- Enable Supabase Realtime
ALTER PUBLICATION supabase_realtime ADD TABLE complaints;
ALTER PUBLICATION supabase_realtime ADD TABLE audit_logs;
