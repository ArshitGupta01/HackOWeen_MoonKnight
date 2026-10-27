# Database and storage

## Current implementation

### 1. Authoritative Case & Legal Workflow Database (SQLDelight / SQLite)
`data/src/commonMain/sqldelight/com/example/theekhai/data/db/Pilot.sq` (with migrations `1.sqm` and `2.sqm`) defines the authoritative workflow schema:
- `PilotCase`: complaint lifecycle (`SUBMITTED`, `ASSIGNED`, `IN_PROGRESS`, `PENDING_VERIFICATION`, `CLOSED`, `REOPENED`), department ownership, SLA deadlines (`dueAt`), optimistic locking (`version`), and escalation level.
- `PilotTask`: coordinated departmental tasks required before closure.
- `PilotEvent`: append-only audit log of every state change, review decision, and SLA breach.
- `PilotReceipt`: idempotency keys (`Idempotency-Key`) preventing duplicate submissions.
- `PilotLink`: duplicate and related-case links.
- `LegalSource`: approved and pending legal/civic reference passages (`CENTRAL` and `HARYANA` packs) with SHA-256 content hashes and superseded-version links.
- `LegalMonitor`: scheduled TinyFish Page (`fetch`) and Topic (`search`) monitors.

Android uses `AndroidSqliteDriver`, iOS uses `NativeSqliteDriver`, and Desktop/Server use `JdbcSqliteDriver`. `THEEKHAI_DB_PATH` selects the server database file; in-memory SQLite is used for tests.

### 2. Connected Supabase PostgreSQL Database (`TheekHai` — `ffmcurazxvygsifbglwy`)
Inspired by TenderLens (`supabase/schema.sql`), the live Supabase project `TheekHai` (`https://ffmcurazxvygsifbglwy.supabase.co`) manages identity, user profiles, bookmarked legal sources, and private evidence storage:
- `public.profiles` (`user_id`, `email`, `full_name`, `organization`, `role`, `department`, `ward`, `preferred_language`, `created_at`, `updated_at`):
  - Synced automatically on `auth.users` signup via `public.handle_new_user()` trigger.
  - Protected by PostgreSQL Row-Level Security (`profiles_select_own`, `profiles_insert_own`, `profiles_update_own` using `(select auth.uid()) = user_id`).
  - Guarded by `public.protect_profile_role()` trigger so authenticated users cannot self-escalate `role` or `department`.
- `public.saved_sources` (`id`, `user_id`, `title`, `source_url`, `jurisdiction`, `snippet`, `created_at`):
  - Bookmarked legal/civic reference sources per user (`unique(user_id, source_url)`).
  - Protected by PostgreSQL RLS (`saved_sources_select_own`, `saved_sources_insert_own`, `saved_sources_delete_own` using `(select auth.uid()) = user_id`).
- `storage.buckets` (`evidence`):
  - Private storage bucket (`public = false`) for complaint evidence attachments.

## Changes and migration rules

- Add SQLDelight migrations under `data/src/commonMain/sqldelight/migrations/` and keep `supabase/schema.sql` aligned with applied Supabase migrations.
- Group status changes, task effects, and audit events in a transaction. Deduplicate retryable requests with an idempotency key and escalation events with a database uniqueness rule.
