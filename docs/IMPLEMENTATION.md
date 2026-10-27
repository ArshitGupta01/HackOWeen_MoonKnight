# Pilot implementation

Implementation started from merge `e060863`. This file records delivered behavior and outstanding verification, not a claim of production readiness.

## Accepted decisions

- Vite/TypeScript DOM website, civic editorial design, English/Hindi and explicitly synthetic municipal configuration.
- Ktor is authoritative; SQLDelight stores workflow. Supabase TheekHai supplies identity and private evidence storage.
- Automatic routing and deadline escalation; independent human closure review and disputed reopening.
- Legal reference & civic guidance uses TinyFish Agent Mode (`/v1/automation/run`) and TinyFish Monitor Mode (`/v1/monitors`), with Tavily Search (`/search` restricted to allowlisted official domains) as discovery and secondary fallback, and approved local SQLite source excerpts (`SOURCES_ONLY`) as deterministic backup fallback. Local LLM inference (`llama.cpp`) is removed.
- TinyFish and Tavily source collection feeds a `PENDING` review queue. No automatic government filing.

## Baseline review

- Baseline server tests: root passed; health failed.
- Server previously called its own complaint endpoint through a client repository.
- Placeholder authentication, fixed sample lists, simulated recordings and fabricated provider outcomes require replacement.
- Docker client is installed; engine was unavailable at planning review. Physical-device and iOS validation require their target environments.

Update this file at each verified checkpoint, including outstanding gaps.

## Checkpoint 1

`gradlew :server:test` passes (health, root, transactional lifecycle/ownership/idempotency and restart/deadline tests). The active server uses local SQLDelight repositories and an online Supabase identity check. Browser sessions are opaque HttpOnly cookies with CSRF checks; sessions expire on server restart. Staff permissions use server-managed `THEEKHAI_STAFF` configuration. Legal source import/review/search is implemented with conservative extractive local inference and no cloud fallback.

## Checkpoint 2

`gradlew :server:test` and `web` production build pass. Local AI (`LOCAL_AI_URL` / `llama.cpp`) is removed and replaced by:
- **TinyFish Agent Mode (`POST /v1/automation/run`)** for source-grounded legal question answering (`TINYFISH_AGENT`) and official page extraction (`POST /api/v1/legal/discover`).
- **TinyFish Monitor Mode (`POST /v1/monitors`, `POST /v1/monitors/{id}/runs`)** persisted in `LegalMonitor` (`GET/POST /api/v1/legal/monitors`, `POST /api/v1/legal/monitors/{id}/run`) for tracking official page (`fetch`) and topic (`search`) updates and queuing changed revisions as `PENDING` for human reviewer approval.
- **Tavily Search (`POST /search`)** restricted to allowlisted official domains for discovery and secondary question-answering fallback (`TAVILY_FALLBACK`).
- **Deterministic Backup Fallback (`SOURCES_ONLY`)** serving seeded/approved Central and Haryana reference packs directly from SQLite when external providers are unconfigured, unreachable, or return ungrounded text.
- Unmounted legacy server route files and greeting scaffolding (`GreetingUtil.kt`) removed; `POST /api/v1/voice/transcribe` mounted directly on the authoritative server. All history consolidated onto the single `main` branch.


## Checkpoint 3

Connected live Supabase project `TheekHai` (`ffmcurazxvygsifbglwy`, `https://ffmcurazxvygsifbglwy.supabase.co`) inspired by TenderLens (`D:\TenderLens`):
- Applied live Supabase migration `theekhai_profiles_and_saved_sources_rls` creating `public.profiles` and `public.saved_sources` with strict Row-Level Security (`(select auth.uid()) = user_id`), `protect_profile_role()` privilege-escalation guard, `handle_new_user()` profile sync trigger, and private `evidence` bucket.
- Provisioned 4 pre-confirmed shared demo accounts (`citizen@theekhai.in`, `pwd@theekhai.in`, `reviewer@theekhai.in`, `admin@theekhai.in` — password `TheekHai@2026`) and `auto_confirm_pilot_user_trigger` for instant citizen registration.
- Wired `/api/v1/auth/config`, `/api/v1/auth/login`, `/api/v1/auth/register`, `/api/v1/auth/recover`, `/api/v1/auth/me`, `/api/v1/profile`, and `/api/v1/saved-sources` in Ktor (`SupabaseJwtAuth.kt` + `Application.kt`) and updated the bilingual web frontend (`/login` with 1-click demo role cards & password visibility toggle, `/signup` with 3-bar password strength meter, `/account` with RLS profile sync & saved sources, and `/library` source bookmarking).

## Checkpoint 4 — Native preview honesty and theme controls (2026-10-28)

- Android and Desktop Compose scaffolds now expose a dark-mode control while following the device theme by default; the browser theme toggle persists its override.
- Removed false native success states for sign-in, signup, report submission, draft saving, voice capture, profile sync, and field evidence. Native preview data is empty or explicitly labeled as illustrative so it cannot be mistaken for a live resident case.
- Verified: web production build and four Playwright checks; `:app:shared:jvmTest`, `:server:test`, Android debug assembly, and Desktop Kotlin compilation pass.
- At that checkpoint, native sign-in and report submission were still unavailable. Checkpoint 5 records their connection; the remaining native gaps are listed there.

## Checkpoint 5 — Native authentication and report API (2026-10-28)

- Native sign-in and citizen registration now call the Ktor Supabase auth bridge. Native clients attach the returned Supabase access token as a Bearer token, so protected requests are verified by the server and the web cookie/CSRF path remains unchanged.
- Native report submission calls `POST /api/v1/complaints`, preserves the explicit consent gate, sends an idempotency key, and displays the authoritative case tracking ID/status returned by Ktor.
- Android debug builds permit local emulator HTTP only; release manifest disables cleartext. Desktop/Android default to the local backend addresses described in the platform data modules.
- Verified Android debug assembly, shared JVM/Android compilation, desktop compilation, server compilation, and web production build.
- Remaining native gaps: process-death session restoration/refresh, server-backed workspace and case details, profile/source sync, actual microphone capture and OpenRouter transcription, photo selection/private evidence upload, and runtime location permissions/reverse-geocoding. The current report API supports coordinates, but the native form does not yet capture or attach them.
