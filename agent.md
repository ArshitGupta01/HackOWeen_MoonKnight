# TheekHai project guide

Last reviewed: 2026-10-28

## Maintenance

Read this guide before changing the project. Maintain a clean repository and Clean Architecture throughout development. Update this file in the same change when architecture, module ownership, setup, dependencies, validation, or established decisions change. Record decisions and current facts, not a transcript or per-edit log. Preserve user changes and keep work scoped.

Current user instruction: implement the accepted English/Hindi civic pilot, common Ktor backend, Supabase `TheekHai` authentication and RLS-protected PostgreSQL database (`public.profiles`, `public.saved_sources`, `public.complaints`, `public.case_tasks`, `public.audit_logs`, `public.legal_sources`, `public.legal_monitors`) inspired by TenderLens, and legal reference & source monitoring powered by TinyFish (Agent Mode and Monitor Mode), Tavily discovery/fallback, and deterministic local approved-source backup fallback (`SOURCES_ONLY`), with no local LLM container. Commit verified increments on the unified `main` branch. See `docs/IMPLEMENTATION.md` for current verification and remaining release gates.

## Current implementation

Repository: https://github.com/ArshitGupta01/HackOWeen_MoonKnight

The project is Kotlin Multiplatform + Compose Multiplatform, Clean Architecture + MVVM, Koin, SQLDelight, a Ktor backend (`:server`), and a Vite + TypeScript + Lenis (`lenis`) bilingual (`en`/`hi`) web frontend (`web/`). Android, iOS, and Desktop JVM source sets exist. Native clients authenticate with Supabase through Ktor using a bearer token held in memory for the app process. Native account/profile and saved-reference management, report creation and triage, live case lists/details/workflow actions, legal guidance search and Q&A, and reverse geocoding use the shared `/api/v1` contract. Provider credentials remain server-side.

The connected Supabase project is named `TheekHai` (project ref `ffmcurazxvygsifbglwy`, active in `ap-northeast-1`, URL `https://ffmcurazxvygsifbglwy.supabase.co`). Following the TenderLens architecture (`supabase/schema.sql`), Supabase provides:
- Authentication (`/auth/v1/token`, `/auth/v1/signup`, `/auth/v1/recover`, `/auth/v1/user`) with 4 pre-confirmed shared pilot demo accounts (`citizen@theekhai.in`, `pwd@theekhai.in`, `reviewer@theekhai.in`, `admin@theekhai.in` — password `TheekHai@2026`) and an automatic confirmation trigger (`auto_confirm_pilot_user_trigger`) for newly registered pilot accounts.
- `public.profiles` (`user_id`, `email`, `full_name`, `organization`, `role`, `department`, `ward`, `preferred_language`) protected by PostgreSQL Row-Level Security (`(select auth.uid()) = user_id`) and a `protect_profile_role()` trigger preventing client-side role escalation.
- `public.saved_sources` (`id`, `user_id`, `title`, `source_url`, `jurisdiction`, `snippet`) protected by RLS (`(select auth.uid()) = user_id`) for bookmarking approved civic/legal reference passages.
- `public.complaints`, `public.case_tasks`, `public.audit_logs`, `public.legal_sources`, and `public.legal_monitors` protected by RLS and synchronized from Ktor workflow actions.
- Private `evidence` Storage bucket (`storage.buckets`).

The Ktor/SQLDelight backend remains the authoritative owner of case workflow (`PilotCase`, `PilotTask`, `PilotEvent`, `PilotReceipt`, `PilotLink`, `LegalSource`, `LegalMonitor`). Base package is `com.example.theekhai`.

See [ARCHITECTURE.md](ARCHITECTURE.md) for the architecture reference and [README.md](README.md) for run instructions. Keep those documents aligned when implementation changes.

The project-specific [security](docs/SECURITY.md), [code style](docs/CODE_STYLE.md), [database](docs/DATABASE.md), [API](docs/API.md), and [design system](docs/DESIGN_SYSTEM.md) guides turn the supplied reference documents into applicable final checks. Update them with real implementation changes.

[Legal channels and integration references](docs/LEGAL_AND_INTEGRATIONS.md) records Haryana AAS as the first government integration to investigate and eCourts as an authorized judicial filing channel requiring appropriate legal review. Internal SLA escalation is separate from an official appeal or court filing.

TinyFish (Agent Mode `/v1/automation/run` and Monitor Mode `/v1/monitors`), Tavily (`/search` restricted to allowlisted official domains), OpenRouter (`/chat/completions`, default free router `openrouter/free`, configurable with `OPENROUTER_MODEL`), and Sarvam AI (`/speech-to-text`, fallback for Whisper / raw audio transcription via `SARVAM_API_KEY`) power official source discovery, scheduled change monitoring, AI complaint intake triage, voice-to-text refinement/transcription, and source-grounded legal/civic question answering behind the Ktor backend (`TINYFISH_API_KEY`, `TAVILY_API_KEY`, `OPENROUTER_API_KEY`, `GROQ_API_KEY`, `SARVAM_API_KEY`). Provider credentials and connectivity details stay server-side; the public health endpoint reports service health only. Geotagging across Web, Android, and Desktop uses `GET /api/v1/geo/reverse` (OpenStreetMap Nominatim + Ward 4 geospatial landmark index) alongside an interactive Ward 4 map pin picker and KMP `expect`/`actual` `PlatformCapabilities`. Local LLM inference (`llama.cpp` / `Qwen3`) has been removed; when external providers are unavailable or ungrounded, `LegalService` falls back deterministically to approved local SQLite source excerpts (`SOURCES_ONLY`) and `LOCAL_RULE_FALLBACK`. Changed or newly discovered sources always enter the `PENDING` review queue rather than overwriting `APPROVED` guidance.

## Required module boundaries

| Module | Responsibility | Allowed project dependencies |
| --- | --- | --- |
| `:core` | Serializable DTOs, API route constants, shared contract utilities | None; serialization is allowed |
| `:domain` | Entities, repository ports, use cases | None; coroutines are allowed |
| `:data` | Repository implementations, SQLDelight, Ktor client, native API/session adapters, data DI modules | `:domain`, `:core` |
| `:app:shared` | Compose presentation, MVVM, client Koin composition root | `:domain`, `:core`, `:data` only from `di/` |
| Platform apps | Entry points, packaging, platform wiring | `:app:shared` |
| `:server` | Ktor HTTP layer, server DI and infrastructure wiring | `:domain`, `:data`, `:core` |

`app/iosApp` is an Xcode project, not a declared Gradle `:app:iosApp` module. The browser-native web frontend (`web/`) is independent of the KMP UI and SQLite modules; it consumes the same REST API.

Never add UI, Ktor, SQLDelight, or Koin to `:domain`. Never make `:core` depend on another project module. Data must not depend on Compose or apps; server must not depend on apps. Keep DTOs separate from domain/UI models when their responsibilities differ.

## Presentation and DI

- ViewModels expose `StateFlow<UiState>` and call use cases only. Stateful `*Route` composables obtain ViewModels through `koinViewModel()`; stateless `*Screen` composables receive state and callbacks.
- Register use cases in `domainModule` and ViewModels with `viewModelOf` in `presentationModule`, under `app/shared/.../di/AppModule.kt`.
- Data exposes `dataModule`, `localDataModule`, `remoteDataModule`, and `expect val platformDataModule`. Actuals supply the SQLite driver and backend URL.
- Client DI starts through `initKoin`: Android `TheekHaiApplication`, desktop entry point, and iOS `startKoinForIos()`.
- Server DI uses `koin-ktor` and `serverModule`; it uses local persistence without calling its own HTTP API.

## One common backend and separate database instances

Android, Desktop, and browser-native web all call the same Ktor backend. iOS remains supported by the existing foundation.

Share the SQLDelight schema/code, not a live database file. Each native client has its own local SQLite instance; the server has its own authoritative persistent database (`Pilot.sq` with migrations `1.sqm` and `2.sqm`). Local SQLite is the source for native UI observation/cache and queued drafts. It cannot authorize or finalize shared workflow transitions.

The server owns permissions, case IDs, assignments, valid status changes, duplicate links, required departmental task checks, deadlines, escalation, closure review, reopening, and append-only audit events. Browser sessions use `HttpOnly` cookies (`theekhai_session`) with `X-CSRF-Token` validation plus `Authorization: Bearer` token fallback, while Ktor forwards the user's Supabase JWT Bearer token when querying Supabase PostgREST (`/rest/v1/profiles`, `/rest/v1/saved_sources`, `/rest/v1/complaints`) so PostgreSQL RLS policies are enforced per user.

## Validation

Use `.\gradlew.bat` on Windows or `./gradlew` on Unix, followed by the appropriate task:

| Purpose | Task |
| --- | --- |
| Android debug build | `:app:androidApp:assembleDebug` |
| Desktop run | `:app:desktopApp:run` |
| Server run | `:server:run` |
| Domain JVM tests | `:domain:jvmTest` |
| Data JVM tests | `:data:jvmTest` |
| Shared JVM tests | `:app:shared:jvmTest` |
| Server tests | `:server:test` |
| Web build | `cd web && npm run build` |
| Ngrok unified tunnel (Web + API) | `.\start-ngrok.ps1` |

## Decisions and remaining gaps

- 2026-10-27: Adopt Clean Architecture + MVVM, Koin, SQLDelight, and common Ktor backend specification.
- 2026-10-27: Native web (`web/`) uses Vite + TypeScript HTML/DOM, separate from Compose and consuming the common REST API.
- 2026-10-27: Server is authoritative for shared complaint workflow (`Pilot.sq`).
- 2026-10-28: Live Supabase project `TheekHai` (`ffmcurazxvygsifbglwy`) is connected for Auth (`/login`, `/signup`, `/recover`), RLS-protected `public.profiles`, `public.saved_sources`, `public.complaints`, `public.case_tasks`, `public.audit_logs`, `public.legal_sources`, `public.legal_monitors`, and private `evidence` Storage bucket, inspired by TenderLens.
