# Security and release gate

This document adapts the user's security reference to TheekHai. It describes required controls, not a claim that they are implemented. Before public deployment, verify each applicable control with a test or review and record any gap in the release notes. The current repository is a greeting demo and does not yet satisfy this gate.

## Trust boundaries

- Supabase project `TheekHai` (`ffmcurazxvygsifbglwy`) is the planned identity and private evidence provider. Its application `public` schema currently has no tables. Do not move case workflow into Supabase. Server-side Ktor and SQLDelight own case records, status transitions, deadlines, and audit history.
- Ktor verifies user identity on every protected request and resolves citizen, department, and reviewer permissions from server-managed data. UI visibility is not authorization. Every case, report, task, evidence, and audit read or mutation must check its resource-level access rule.
- Supabase Storage buckets for evidence must be private, with explicit scoped access policies or server-authorized temporary links. If any Supabase tables become exposed through its Data API, enable and test RLS for each table. SQLite has no RLS; Ktor must enforce access before querying it.
- Use Supabase Auth for password storage, email verification, session expiry and recovery. Do not implement a second password database. Require MFA for staff/reviewer accounts before public launch; define recovery and offboarding. Use server-managed roles, not user-editable metadata.
- Browser sessions must avoid persistent JavaScript-accessible token storage. Use a same-origin session bridge with `HttpOnly`, `Secure`, `SameSite` cookies and CSRF protection for state changes; validate the exact deployment topology. Native clients may use their platform's secure credential store. Sign-out and role removal must invalidate effective access.

## Request and data protection

- Validate payload type, length, ranges, enum values and unexpected fields in Ktor. Use SQLDelight-bound parameters; never concatenate user data into SQL or shell commands. Render user text as text; sanitize any permitted rich text. Guard outbound URLs against SSRF and allowlist service origins.
- Validate evidence extension, declared MIME type, file signature, and size. Generate server-owned object names, keep evidence private, scan uploads before making them available, and reject unsafe content. Persist case/attachment ownership and access decisions server-side.
- Apply rate limits to intake, sign-in/recovery via provider controls, searches, uploads, and AI routes. Verify any future webhook signature and timestamp before acting; if there are no webhooks, this item is not applicable.
- Keep server-only Supabase credentials, Hugging Face tokens, signing material, and DB paths out of browser bundles and Git. Provide `.env.example` with variable names and safe placeholders only. Never log secrets, full tokens, evidence contents, or unnecessary personal details.
- Production uses HTTPS, trusted-origin CORS, restrictive cookie settings, safe response headers, and sanitized errors. Disable public production source maps. Restrict direct database access and back up the server SQLite database and evidence metadata.

## Final verification

- [ ] Authentication, email verification, staff MFA, expiry, logout and role removal tested.
- [ ] Resource access tested for citizen, department, reviewer, anonymous, and cross-case requests.
- [ ] Cookie and CSRF behavior tested on every state-changing browser request; no auth tokens in browser storage or production logs.
- [ ] SQL injection, XSS, SSRF, malformed input, and concurrency cases tested.
- [ ] Upload size/type/signature, malware scan, private access, and temporary-link expiry tested.
- [ ] Supabase Storage policies reviewed; RLS tested for any exposed Supabase tables. SQLite access enforced in Ktor.
- [ ] Rate limits, HTTPS, CORS, security headers, error redaction, and source-map policy verified in the deployed configuration.
- [ ] Dependency and secret scans run; default credentials absent; webhook checks marked applicable or not applicable.
- [ ] Backup and restore tested; audit history preserved across restarts and migrations.

Do not mark a box complete based only on documentation or a successful build. Record the exact test and environment when the release gate is performed.
