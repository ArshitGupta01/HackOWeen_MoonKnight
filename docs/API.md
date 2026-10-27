# API contract

The Ktor API is JSON at `/api/v1`, defined by `:core`'s `PilotDto.kt` and mounted in `server/src/main/kotlin/com/example/theekhai/Application.kt`.

## Authentication, Profile & Saved Sources (Supabase `TheekHai` + RLS)

- `GET /api/v1/providers` -> `ProviderStatusDto` (`supabaseConfigured`, `tinyFishConfigured`, `tavilyConfigured`, `openRouterConfigured`, `sarvamConfigured`, `groqConfigured`).
- `GET /api/v1/auth/config` -> `AuthConfigDto` (`supabaseUrl`, `publishableKey`, `configured`).
- `POST /api/v1/auth/login` -> Authenticates directly via email and password (or instant demo credentials), resolves role/profile from `public.profiles` or local database, sets an `HttpOnly` cookie (`theekhai_session`), and returns `accessToken` for Bearer fallback.
- `POST /api/v1/auth/register` -> Validates email syntax and deliverability, prevents disposable/fake domains, dispatches an official email verification link via Gmail API (`GmailService`, sender strictly `arshitgupta34@gmail.com`) and Supabase `/auth/v1/signup`, stores pending registration, and returns `RegisterResponseDto` (`status: "verification_sent"`).
- `POST /api/v1/auth/verify-email` -> Verifies the registration token, creates the citizen account in SQLDelight and Supabase, issues the authenticated `theekhai_session` HttpOnly cookie and Bearer token, and returns `VerifyEmailResponseDto`.
- `GET /api/v1/auth/verify-email?token=...` -> Direct link verification endpoint from email click; sets session cookie and redirects browser to `/verify-email?token=...&status=success` or responds with JSON.
- `POST /api/v1/auth/resend-verification` -> Resends the email verification link to an unverified registration email address.
- `POST /api/v1/auth/instant-login` -> 1-click instant login for the 10 pre-seeded pilot demo accounts (`Mohit`, `Arshit`, `Atri`, etc.).
- `POST /api/v1/auth/recover` -> Triggers a Supabase password reset email via `/auth/v1/recover`.
- `GET /api/v1/auth/me` -> Returns `IdentityDto` (`id`, `role`, `department`, `csrf`, `email`, `fullName`, `organization`, `ward`, `preferredLanguage`).
- `POST /api/v1/auth/logout` -> Invalidates the server session and clears `theekhai_session` (requires `X-CSRF-Token`).
- `GET /api/v1/profile` & `POST /api/v1/profile` -> Reads/updates the user's RLS-protected `public.profiles` record in Supabase using the session's Bearer JWT.
- `GET /api/v1/saved-sources`, `POST /api/v1/saved-sources`, `DELETE /api/v1/saved-sources/{id}` -> Lists, bookmarks, and deletes RLS-protected `public.saved_sources` rows in Supabase.

## Case Workflow, Verification & Priority (`/api/v1/complaints`)

- `GET /api/v1/complaints?offset=0` -> Paginated `CasePageDto` filtered by actor role/ownership.
- `POST /api/v1/complaints/triage` -> Analyzes complaint intake (`TriageAssistRequestDto`) via OpenRouter (`/chat/completions`) and returns `TriageAssistResponseDto` (`suggestedTitle`, `suggestedCategory`, `suggestedPriority`, `suggestedDepartment`, `duplicateCaseIds`, `summary`, `provider`), falling back deterministically to `LOCAL_RULE_FALLBACK` when offline.
- `POST /api/v1/complaints` -> Submits a new complaint (`ReportDto` with `priority`) with `Idempotency-Key` and `X-CSRF-Token`, persisting to SQLDelight and syncing to Supabase `public.complaints`, `public.case_tasks`, and `public.audit_logs`. Computes SLA deadlines dynamically (`CRITICAL` = 4h, `HIGH` = 24h, `MEDIUM` = 72h, `LOW` = 168h).
- `GET /api/v1/complaints/{id}` -> Returns `CaseDetailDto` (`case`, `tasks`, `events`, `links`).
- `POST /api/v1/complaints/{id}/actions` -> Executes workflow transitions (`START`, `ADD_TASK`, `COMPLETE_TASK`, `REQUEST_CLOSURE`, `APPROVE_CLOSURE`, `REJECT_CLOSURE`, `CHALLENGE`, `REOPEN`, `LINK_DUPLICATE`, `LINK_RELATED`) with optimistic concurrency (`version`) and separation of duties.
- `POST /api/v1/complaints/{id}/verify` -> Autonomous municipal resolution verification via TinyFish Agent Mode.
- `POST /api/v1/complaints/{id}/resolve` -> Worker completion and closure request.
- `POST /api/v1/complaints/{id}/escalate` -> Manual operational escalation.
- `POST /api/v1/evidence/verify` -> Field worker geofence validation (Haversine formula) and visual evidence analysis.
- `GET /api/v1/audit/logs` -> Case audit events.
- `POST /api/v1/sync/batch` -> Offline sync batch ingest.

## Gmail & Official Civic Grievance Dispatch (`/api/v1/gmail/*` & `/api/v1/complaints/{id}/email/*`)

- `GET /api/v1/gmail/status` -> Returns `GmailOAuthStatusDto` (`configured`, `connected`, `senderEmail = "arshitgupta34@gmail.com"`, `authUrl`).
- `GET /api/v1/gmail/auth-url?redirect_uri=...` -> Returns Google OAuth consent URL for `https://www.googleapis.com/auth/gmail.send` scope.
- `GET /api/v1/gmail/oauth2callback?code=...` -> Google OAuth callback handler that exchanges authorization code for refresh/access tokens and persists `.gmail_tokens.json`.
- `POST /api/v1/complaints/{id}/email/draft` -> Uses TinyFish Agent Mode to discover the responsible government authority and email from official `.gov.in`/`.nic.in` portals (with verified Haryana civic directory fallback), then drafts a formal statutory civic grievance petition via OpenRouter (or grounded legal template). Sender is strictly hardcoded to `arshitgupta34@gmail.com`. Returns `EmailDraftDto`.
- `POST /api/v1/complaints/{id}/email/send` -> Dispatches the reviewed grievance email from `arshitgupta34@gmail.com` to the official authority via Gmail API (`users/me/messages/send`), records a `GRIEVANCE_EMAILED` event on the case timeline, and returns `SendEmailResponseDto`.

## Legal Reference, TinyFish Agent/Monitor, OpenRouter & Tavily (`/api/v1/legal/*`)

- `GET /api/v1/legal/sources?q=...&jurisdiction=CENTRAL|HARYANA` -> Searches approved sources (with English/Hindi glossary expansion).
- `POST /api/v1/legal/ask` -> Answers a civic/legal question using `TINYFISH_AGENT` -> `TAVILY_AND_OPENROUTER` / `OPENROUTER_SYNTHESIS` / `TAVILY_FALLBACK` -> `SOURCES_ONLY` backup fallback.
- `POST /api/v1/legal/discover` -> Reviewer/Admin endpoint using Tavily official-domain discovery and TinyFish Agent extraction to queue `PENDING` sources.
- `GET /api/v1/legal/monitors`, `POST /api/v1/legal/monitors`, `POST /api/v1/legal/monitors/{id}/run` -> Creates and executes TinyFish Page (`fetch`) and Topic (`search`) monitors.
- `GET /api/v1/legal/review`, `POST /api/v1/legal/sources`, `POST /api/v1/legal/sources/{id}/review` -> Reviewer approval/rejection queue.
- `POST /api/v1/voice/transcribe` -> Transcribes and refines recorded voice notes (`TranscribeRequestDto` with `audioBase64`, `mimeType`, `language`, `transcriptHint`) via Groq Whisper / Sarvam AI / OpenRouter into structured `TranscribeResponseDto`.
- `GET /api/v1/geo/reverse?lat=...&lon=...&lang=en|hi` -> Reverse-geocodes GPS or map-picked coordinates via OpenStreetMap Nominatim + Murthal geospatial index (`map.osm`) into `ReverseGeocodeResponseDto`.


## Civic Accountability Leaderboard & Instant Sign-In (`/api/v1/leaderboard` & `/api/v1/auth/*`)

- `GET /api/v1/leaderboard?role=ALL|CITIZEN|AUTHORITY` -> Returns ranked participants (`LeaderboardEntryDto`) with scores, action badges, and verified activity breakdown, along with the recent point-award activity ledger (`ScoreEventDto`).
- `GET /api/v1/auth/dummy-users` -> Returns credentials and metadata for all 10 pre-seeded pilot users (`Mohit`, `Arshit`, `Atri`, `Himanshu`, `Priya`, `Rohan`, `Ananya`, `Vikram`, `Neha`, `Kabir`).
- `POST /api/v1/auth/instant-login` -> Performs 1-click credential-free authentication for any seeded dummy account, issuing a local session cookie and `th_local_...` bearer token.
