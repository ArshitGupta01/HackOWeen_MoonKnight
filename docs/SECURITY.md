# Security and release gate

This document adapts the user's security reference to TheekHai. Before public deployment, verify each applicable control with a test or review and record any gap in the release notes.

## Trust boundaries

- Supabase project `TheekHai` (`ffmcurazxvygsifbglwy`, `https://ffmcurazxvygsifbglwy.supabase.co`) is the connected identity, RLS profile/bookmark (`public.profiles`, `public.saved_sources`), and private evidence (`storage.buckets` -> `evidence`) provider. Case workflow (`PilotCase`, `PilotTask`, `PilotEvent`, `PilotReceipt`, `PilotLink`) remains owned by server-side Ktor and SQLDelight.
- Ktor verifies user identity on every protected request via Supabase Auth (`/auth/v1/user`) and resolves citizen, department, reviewer, and admin permissions from `THEEKHAI_STAFF` or the database-protected `public.profiles` row (`protect_profile_role()` trigger prevents users from self-escalating roles via PostgREST). UI visibility is not authorization. Every case, report, task, evidence, and audit read or mutation checks its resource-level access rule.
- `public.profiles` and `public.saved_sources` enforce PostgreSQL Row-Level Security (`(select auth.uid()) = user_id`) for `select`, `insert`, `update`, and `delete`. Ktor queries PostgREST using the authenticated user's Supabase JWT Bearer token so RLS is enforced on every query. SQLite has no RLS; Ktor enforces access before querying it.
- Supabase Auth handles password storage, registration (`/api/v1/auth/register`), sign-in (`/api/v1/auth/login`), and password recovery (`/api/v1/auth/recover`).
- Browser sessions avoid JavaScript-accessible token storage. Ktor issues an opaque `HttpOnly` cookie (`theekhai_session`, `SameSite=Strict`) and enforces `X-CSRF-Token` on state-changing requests (`POST`, `DELETE`). Sign-out (`POST /api/v1/auth/logout`) invalidates the server session and clears the cookie.
- Native Android/Desktop clients use the API's Bearer-token path. The current native token holder is process-memory only and is lost on restart; do not persist it in plain preferences. Android cleartext is enabled only for debug emulator access to the local Ktor server and disabled in release manifests. Production clients must use HTTPS.

## Request and data protection

- Validate payload type, length, ranges, enum values and unexpected fields in Ktor. Use SQLDelight-bound parameters; never concatenate user data into SQL or shell commands. Guard outbound URLs against SSRF and allowlist official service origins (`LegalService.allowedHosts`).
- Keep server-only credentials, TinyFish/Tavily API keys, and DB paths out of browser bundles and Git. `.env` is gitignored and `.env.example` provides variable names and safe placeholders.
- Production uses HTTPS (`THEEKHAI_COOKIE_SECURE=true`), trusted-origin CORS (`THEEKHAI_ALLOWED_ORIGINS`), restrictive cookie settings, `X-Content-Type-Options: nosniff`, `Cache-Control: no-store`, and sanitized errors (`ApiErrorDto`).

## Final verification

- [x] Supabase Auth sign-in, registration, password recovery, session lookup, and logout wired through Ktor `HttpOnly` + CSRF bridge.
- [x] Resource access tested for citizen, department, reviewer, and cross-case requests (`WorkflowTest`).
- [x] Cookie and CSRF behavior tested on state-changing browser requests (`WorkflowTest`).
- [x] Supabase RLS enabled and verified on `public.profiles` and `public.saved_sources`, with `protect_profile_role()` trigger preventing privilege escalation.
- [x] Login authentication secured with two-factor email verification (6-digit OTP dispatched via Gmail API / Supabase Auth OTP, verified via `/auth/login/verify-code`).
- [ ] Production malware scanning for evidence uploads before public launch.
