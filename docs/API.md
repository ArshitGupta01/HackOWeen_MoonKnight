# API contract

The current Ktor API is JSON at `/api/v1`. Today it exposes `GET /api/v1/greeting?platform=...`, defined by `:core`'s `ApiRoutes` and `GreetingDto`. The server also has a root greeting route. The complaint API described below is planned and must not be presented as implemented.

## Planned contract rules

- Browser, Android, Desktop, and future iOS clients call the same Ktor backend. New public DTOs and route constants belong in `:core`; keep browser TypeScript definitions or generated clients aligned with the actual wire contract.
- Protected endpoints require a verified identity. Ktor enforces the role and access for each resource. Use HTTP methods and resource names consistently; version breaking changes under a new API prefix.
- Use consistent JSON errors with a stable code, safe message, and request ID. Return suitable `400`, `401`, `403`, `404`, `409`, `413`, `429`, and `500` statuses. Never return raw exception details.
- Use pagination for lists; validate filters and sorting. Use an idempotency key for submissions and conflict detection for concurrent workflow edits.
- Document actual request/response fields, authentication, rate limits, and examples alongside route implementation, ideally in an OpenAPI document checked against tests.
- Document Supabase Auth/Storage, TinyFish (`TINYFISH_API_KEY`), and Tavily (`TAVILY_API_KEY`) integration boundaries, environment variable names, and degraded behavior (`SOURCES_ONLY` backup fallback). Provider secrets remain server-side.
- TinyFish (Agent Mode `/v1/automation/run` and Monitor Mode `/v1/monitors`) and Tavily (`/search` restricted to allowlisted official domains) are integrated behind `/api/v1/legal/*` (`GET /api/v1/legal/sources`, `POST /api/v1/legal/ask`, `POST /api/v1/legal/discover`, `GET/POST /api/v1/legal/monitors`, `POST /api/v1/legal/monitors/{id}/run`, `GET /api/v1/legal/review`, `POST /api/v1/legal/sources/{id}/review`).

## Planned resource families

`/api/v1/cases`, `/api/v1/cases/{id}`, case reports/duplicates/related cases, assignments and tasks, evidence and closure requests, reviews and challenges, deadlines/escalations, and operational summaries. Exact child-route paths and DTOs must be recorded here when implemented; this list is not a claim that those endpoints exist.
