# TheekHai project guide

Last reviewed: 2026-10-27

## Maintenance

Read this guide before changing the project. Maintain a clean repository and Clean Architecture throughout development. Update this file in the same change when architecture, module ownership, setup, dependencies, validation, or established decisions change. Record decisions and current facts, not a transcript or per-edit log. Preserve user changes and keep work scoped.

Current user instruction: implement the accepted English/Hindi civic pilot, common Ktor backend, and legal reference & source monitoring powered by TinyFish (Agent Mode and Monitor Mode), Tavily discovery/fallback, and deterministic local approved-source backup fallback (`SOURCES_ONLY`), with no local LLM container. Commit verified increments on `codex/theekhai-project-guides`; retain Mohit's history. See `docs/IMPLEMENTATION.md` for current verification and remaining release gates.

## Current implementation

Repository: https://github.com/ArshitGupta01/HackOWeen_MoonKnight

Reviewed upstream commit: `d28e64b` (Architectural setup). The project is Kotlin Multiplatform + Compose Multiplatform, Clean Architecture + MVVM, Koin, SQLDelight, and a Ktor backend. Android, iOS, and Desktop JVM source sets exist. The previous Compose web app has been removed; a browser-native frontend is not implemented yet.

The connected Supabase project is named `TheekHai` (project ref `ffmcurazxvygsifbglwy`, active in `ap-northeast-1`). It is reserved for authentication and private evidence storage; no application tables were found in its `public` schema at this review. The Ktor/SQLDelight backend remains the intended owner of case workflow and case records. Supabase integration has not yet been implemented.

The working example is greeting creation/history, not the complaint workflow. It includes shared DTOs/routes, domain use cases, SQLite persistence, a Ktor client, ViewModel/Route/Screen separation, and platform/server DI. Tests exist but have not been executed in this workspace. Base package is `com.example.theekhai`.

See [ARCHITECTURE.md](ARCHITECTURE.md) for the existing architecture reference and [README.md](README.md) for run instructions. Keep those documents aligned when implementation changes.

The project-specific [security](docs/SECURITY.md), [code style](docs/CODE_STYLE.md), [database](docs/DATABASE.md), [API](docs/API.md), and [design system](docs/DESIGN_SYSTEM.md) guides turn the supplied reference documents into applicable final checks. Update them with real implementation changes; do not mark planned controls as deployed.

[Legal channels and integration references](docs/LEGAL_AND_INTEGRATIONS.md) records Haryana AAS as the first government integration to investigate and eCourts as an authorized judicial filing channel requiring appropriate legal review. No API access, integration permission, outreach, or filing is established by those references. Internal SLA escalation is separate from an official appeal or court filing.

TinyFish (Agent Mode `/v1/automation/run` and Monitor Mode `/v1/monitors`) and Tavily (`/search` restricted to allowlisted official domains) power official source discovery, scheduled change monitoring, and source-grounded legal/civic question answering behind the Ktor backend (`TINYFISH_API_KEY`, `TAVILY_API_KEY`). Local LLM inference (`llama.cpp` / `Qwen3`) has been removed; when external providers are unavailable or ungrounded, `LegalService` falls back deterministically to approved local SQLite source excerpts (`SOURCES_ONLY`). Changed or newly discovered sources always enter the `PENDING` review queue rather than overwriting `APPROVED` guidance.

## Required module boundaries

| Module | Responsibility | Allowed project dependencies |
| --- | --- | --- |
| `:core` | Serializable DTOs, API route constants, shared contract utilities | None; serialization is allowed |
| `:domain` | Entities, repository ports, use cases | None; coroutines are allowed |
| `:data` | Repository implementations, SQLDelight, Ktor client, data DI modules | `:domain`, `:core` |
| `:app:shared` | Compose presentation, MVVM, client Koin composition root | `:domain`, `:core`, `:data` only from `di/` |
| Platform apps | Entry points, packaging, platform wiring | `:app:shared` |
| `:server` | Ktor HTTP layer, server DI and infrastructure wiring | `:domain`, `:data`, `:core` |

`app/iosApp` is an Xcode project, not a declared Gradle `:app:iosApp` module. The browser-native web frontend is independent of the KMP UI and SQLite modules; it consumes the same REST API.

Never add UI, Ktor, SQLDelight, or Koin to `:domain`. Never make `:core` depend on another project module. Data must not depend on Compose or apps; server must not depend on apps. Keep DTOs separate from domain/UI models when their responsibilities differ.

Gradle dependency declarations enforce module visibility, but cannot enforce the `:data` imports only in `app/shared/.../di/` package convention. Review imports explicitly. The current domain module has no `:core` dependency; do not infer one from the diagram in ARCHITECTURE.md.

## Presentation and DI

- ViewModels expose `StateFlow<UiState>` and call use cases only. Stateful `*Route` composables obtain ViewModels through `koinViewModel()`; stateless `*Screen` composables receive state and callbacks.
- Register use cases in `domainModule` and ViewModels with `viewModelOf` in `presentationModule`, under `app/shared/.../di/AppModule.kt`.
- Data exposes `dataModule`, `localDataModule`, `remoteDataModule`, and `expect val platformDataModule`. Actuals supply the SQLite driver and backend URL.
- Client DI starts through `initKoin`: Android `TheekHaiApplication`, desktop entry point, and iOS `startKoinForIos()`.
- Server DI uses `koin-ktor` and `serverModule`; it uses local persistence without calling its own HTTP API.
- Use ordinary interfaces for location, file selection, camera/audio capture, and other external capabilities. Keep platform implementations in the relevant source sets. Do not create expect/actual declarations for every service.
- Share forms, lists, timelines, and UI state across Kotlin apps; design phone and desktop layouts deliberately. Device permissions and capabilities still need platform adapters.

## One common backend and separate database instances

Android, Desktop, and browser-native web all call the same Ktor backend. iOS remains supported by the existing foundation but is deferred for the prototype described in the supplied planning text.

Share the SQLDelight schema/code, not a live database file. Each native client has its own local SQLite instance; the server has its own authoritative persistent database. Local SQLite is the source for native UI observation/cache and queued drafts. It cannot authorize or finalize shared workflow transitions.

The server owns permissions, case IDs, assignments, valid status changes, duplicate links, required departmental task checks, deadlines, escalation, closure review, reopening, and audit events. Share pure rules/use cases in domain where appropriate, but always enforce consequential decisions on the server. Web calls REST and never accesses SQLite directly or requires a Kotlin-to-JavaScript bridge.

The greeting demo currently generates a greeting locally on remote failure. Do not copy this fallback into complaint submission, assignment, verification, or closure. Offline drafts must be visibly pending until acknowledged by the server. Define retry/idempotency and synchronization behavior before implementing offline writes.

- Contract: `core/.../contract/GreetingDto.kt`, `ApiRoutes.kt`; currently `GET /api/v1/greeting?platform=...`.
- Schema: `data/src/commonMain/sqldelight/com/example/theekhai/data/db/Greeting.sq`. Add migrations under the SQLDelight source tree and validate upgrades without losing existing records.
- Drivers: Android `AndroidSqliteDriver`, iOS `NativeSqliteDriver`, Desktop/Server `JdbcSqliteDriver`.
- Development URLs: Android emulator `http://10.0.2.2:8080`; Desktop/iOS simulator `http://localhost:8080`. Browser API origin must be configured for its deployment; configure CORS if origins differ. Production uses HTTPS.
- Server database override: `THEEKHAI_DB_PATH`. Persistent evidence files also need durable storage; do not put uploads in the repository.
- Paid AI calls and provider secrets belong on the backend. Client validation improves usability but never replaces server enforcement.

## Prototype scope and workflow

The supplied planning text uses a 10-hour prototype constraint. Prioritize one complete report-to-resolution flow before optional features.

Build: persistent complaint intake (description, category, address/location, optional evidence), deterministic duplicate suggestions, explicit related-case links, department routing or visible triage, lead department plus required tasks, persisted SLA deadlines, closure request, reviewer approval/rejection, reopening, and append-only audit history.

- Preserve original reports when linking duplicates. Related cases remain independently actionable.
- A department requests closure; a reviewer approves or rejects it. Required tasks must be complete before closure.
- Lifecycle: `SUBMITTED -> ASSIGNED -> IN_PROGRESS -> PENDING_VERIFICATION -> CLOSED`. Rejected verification returns work to an active state with a reason; an upheld challenge can reopen a closed case. Specify exact transitions before implementing them.
- Keep lifecycle separate from SLA state and escalation level. A missed deadline escalates an open case; it does not reopen it.
- Store deadlines and run checks on the server, recovering overdue work after restart. Record each breach escalation once per applicable deadline/level, including after retries/restarts.
- Reopening retains original intake and breach history, restores ownership, and creates a corrective-action deadline.
- Evidence review is a human decision for the prototype. AI may flag a contradiction, not certify proof of repair.

After the core works: editable voice transcription with text fallback, user-confirmed AI structuring, AI duplicate ranking, in-app updates, and an operational dashboard for overdue work and pending review.

Defer unrelated service explorers, predictive maintenance, automatic proof of repair, new iOS features, and legal-compliance simulators. TinyFish-assisted portal navigation or automation is a future candidate requiring a defined authorized workflow before implementation. Never invent applicant identity or eligibility data; label synthetic test data clearly.

## Adding a feature

1. Define workflow rules, authorization, state transitions, and acceptance conditions.
2. `:core`: add DTOs, route constants, and consistent error contracts for API changes. Keep web contract documentation aligned; JS/TS does not automatically compile against Kotlin DTOs.
3. `:domain`: add entity, repository port, use case, and meaningful tests with fakes.
4. `:data`: add schema/queries and migrations, sources, mappers, repository implementation, and DI registration. Verify persistence and offline behavior.
5. `:app:shared`: add UiState, ViewModel, Route, Screen, and DI registration. Keep data imports in `di/`.
6. `:server`: expose the use case through an authorized route and server DI; test API rules and persistence.
7. Browser frontend: consume the same endpoint/status/error contract when the feature applies to web.
8. Verify the affected flow and update this guide and related documentation.

## Repository hygiene and design skills

Use feature-oriented packages within the existing modules, small cohesive files, descriptive names, and consistent Kotlin conventions. Avoid empty scaffolding, global mutable state, unused abstractions, duplicate assets, and unrelated formatting churn. Preserve structured concurrency and cancellation; do not silently swallow operational failures.

Centralize versions in `gradle/libs.versions.toml`. Keep wrapper/source assets tracked. Exclude credentials, local config, build outputs, dependency caches, IDE files, uploads, and temporary artifacts; extend `.gitignore` for new tools. Inspect diff and status before finishing. Do not claim checks that were not run.

Use relevant skills from https://github.com/freshtechbro/claudedesignskills as requested. The 22 design/animation/3D skills are available here. Adapt principles to Compose for Kotlin apps and browser-native code for web; choose effects for accessibility, performance, and actual feature needs. Do not replace the established stack merely to use a library from a skill.

## Validation

Use `.\gradlew.bat` on Windows or `./gradlew` on Unix, followed by the appropriate task. These commands are documented in the repository but have not been run in this workspace:

| Purpose | Task |
| --- | --- |
| Android debug build | `:app:androidApp:assembleDebug` |
| Desktop run | `:app:desktopApp:run` |
| Server run | `:server:run` |
| Domain JVM tests | `:domain:jvmTest` |
| Data JVM tests | `:data:jvmTest` |
| Shared JVM tests | `:app:shared:jvmTest` |
| Server tests | `:server:test` |
| Shared Android host tests | `:app:shared:testAndroidHostTest` |
| Shared iOS simulator tests | `:app:shared:iosSimulatorArm64Test` |

Android requires its SDK; iOS requires macOS/Xcode. Verify task availability after build changes. There are no current Compose web tasks or standalone web build commands. Documentation-only changes require path/link and whitespace checks rather than app builds.

## Decisions and remaining gaps

- 2026-10-27: Adopt the user's Clean Architecture + MVVM, Koin, SQLDelight, and common Ktor backend specification. Preserve the implemented module foundation.
- 2026-10-27: Native web means browser HTML/DOM, separate from Compose and consuming the common REST API. Focus prototype delivery on Android, Desktop, and web; retain the iOS foundation.
- 2026-10-27: Server is authoritative for shared complaint workflow. Device SQLite is local persistence, not independent shared workflow authority.
- Still to implement: complaint domain/schema/API/UI, authentication/authorization, synchronization policy, durable evidence handling, deadline worker, review/reopening, browser frontend, and cross-origin API configuration as required. Current source review is not runtime verification.
- 2026-10-27: Use the existing Supabase project named `TheekHai` for planned Auth and private evidence Storage. Final security, code, data, API, and UI checks are tracked in `docs/` and must be evaluated against the implementation before release.
