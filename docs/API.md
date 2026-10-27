# API contract

The Ktor API is JSON at `/api/v1`, defined by `:core`'s `PilotDto.kt` and mounted in `server/src/main/kotlin/com/example/theekhai/Application.kt`.

`GET /health` returns only basic service health (`status`, `version`). Provider configuration and connectivity are server-side details.

## Authentication, Profile & Saved Sources (Supabase `TheekHai` + RLS)

- `POST /api/v1/auth/login` -> Authenticates, resolves the role/profile, and sets an `HttpOnly` cookie (`theekhai_session`) for browser sessions. Native clients without an `Origin` receive an `accessToken` for their in-memory Bearer session.
- `POST /api/v1/auth/register` -> Registers a citizen account, provisions the profile, and sets `theekhai_session`. Browser sessions receive only the cookie; native clients receive an `accessToken` for their in-memory Bearer session.
- `POST /api/v1/auth/recover` -> Triggers a Supabase password reset email via `/auth/v1/recover`.
- `GET /api/v1/auth/me` -> Returns `IdentityDto` (`id`, `role`, `department`, `csrf`, `email`, `fullName`, `organization`, `ward`, `preferredLanguage`).
- `POST /api/v1/auth/logout` -> Invalidates the server session and clears `theekhai_session` (requires `X-CSRF-Token`).
- `GET /api/v1/profile` & `POST /api/v1/profile` -> Reads/updates the user's RLS-protected `public.profiles` record in Supabase using the session's Bearer JWT.
- `GET /api/v1/saved-sources`, `POST /api/v1/saved-sources`, `DELETE /api/v1/saved-sources/{id}` -> Lists, bookmarks, and deletes RLS-protected `public.saved_sources` rows in Supabase.

## Case Workflow & OpenRouter AI Triage (`/api/v1/complaints`)

- `GET /api/v1/complaints?offset=0` -> Paginated `CasePageDto` filtered by actor role/ownership.
- `POST /api/v1/complaints/triage` -> Analyzes complaint intake (`TriageAssistRequestDto`) via OpenRouter (`/chat/completions`) and returns `TriageAssistResponseDto` (`suggestedTitle`, `suggestedCategory`, `suggestedPriority`, `suggestedDepartment`, `duplicateCaseIds`, `summary`, `provider`), falling back deterministically to `LOCAL_RULE_FALLBACK` when offline.
- `POST /api/v1/complaints` -> Submits a new complaint (`ReportDto`) with `Idempotency-Key` and `X-CSRF-Token`, persisting to SQLDelight and syncing to Supabase `public.complaints`, `public.case_tasks`, and `public.audit_logs`.
- `GET /api/v1/complaints/{id}` -> Returns `CaseDetailDto` (`case`, `tasks`, `events`).
- `POST /api/v1/complaints/{id}/actions` -> Executes workflow transitions (`START`, `ADD_TASK`, `COMPLETE_TASK`, `REQUEST_CLOSURE`, `APPROVE_CLOSURE`, `REJECT_CLOSURE`, `CHALLENGE`, `REOPEN`) with optimistic concurrency (`version`) and separation of duties.

## Legal Reference, TinyFish Agent/Monitor, OpenRouter & Tavily (`/api/v1/legal/*`)

- `GET /api/v1/legal/sources?q=...&jurisdiction=CENTRAL|HARYANA` -> Searches approved sources (with English/Hindi glossary expansion).
- `POST /api/v1/legal/ask` -> Answers a civic/legal question using `TINYFISH_AGENT` -> `TAVILY_AND_OPENROUTER` / `OPENROUTER_SYNTHESIS` / `TAVILY_FALLBACK` -> `SOURCES_ONLY` backup fallback.
- `POST /api/v1/legal/discover` -> Reviewer/Admin endpoint using Tavily official-domain discovery and TinyFish Agent extraction to queue `PENDING` sources.
- `GET /api/v1/legal/monitors`, `POST /api/v1/legal/monitors`, `POST /api/v1/legal/monitors/{id}/run` -> Creates and executes TinyFish Page (`fetch`) and Topic (`search`) monitors.
- `GET /api/v1/legal/review`, `POST /api/v1/legal/sources`, `POST /api/v1/legal/sources/{id}/review` -> Reviewer approval/rejection queue.
- `POST /api/v1/voice/transcribe` -> Transcribes and refines recorded voice notes (`TranscribeRequestDto` with `audioBase64`, `mimeType`, `language`, `transcriptHint`) via OpenRouter AI into structured `TranscribeResponseDto` (`text`, `suggestedTitle`, `suggestedCategory`, `provider`).
- `GET /api/v1/geo/reverse?lat=...&lon=...&lang=en|hi` -> Reverse-geocodes GPS or map-picked coordinates via OpenStreetMap Nominatim + Ward 4 geospatial landmark index into `ReverseGeocodeResponseDto` (`latitude`, `longitude`, `address`, `ward`, `landmark`, `source`).
