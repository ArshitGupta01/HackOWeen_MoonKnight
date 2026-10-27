# Pilot implementation

Implementation started from merge `e060863`. This file records delivered behavior and outstanding verification, not a claim of production readiness.

## Accepted decisions

- Vite/TypeScript DOM website, civic editorial design, English/Hindi and explicitly synthetic municipal configuration.
- Ktor is authoritative; SQLDelight stores workflow. Supabase TheekHai supplies identity and private evidence storage.
- Automatic routing and deadline escalation; independent human closure review and disputed reopening.
- Local Qwen3 0.6B via private llama.cpp Docker service; approved central/Haryana source packs with citation-first search.
- TinyFish source collection feeds a review queue. No automatic government filing.

## Baseline review

- Baseline server tests: root passed; health failed.
- Server previously called its own complaint endpoint through a client repository.
- Placeholder authentication, fixed sample lists, simulated recordings and fabricated provider outcomes require replacement.
- Docker client is installed; engine was unavailable at planning review. Physical-device and iOS validation require their target environments.

Update this file at each verified checkpoint, including outstanding gaps.

## Checkpoint 1

`gradlew :server:test` passes (health, root, transactional lifecycle/ownership/idempotency and restart/deadline tests). The active server uses local SQLDelight repositories and an online Supabase identity check. Browser sessions are opaque HttpOnly cookies with CSRF checks; sessions expire on server restart. Staff permissions use server-managed `THEEKHAI_STAFF` configuration. Legal source import/review/search is implemented with conservative extractive local inference and no cloud fallback.

Still pending: legacy native contract cleanup, production auth lifecycle/MFA, rate limits, evidence storage, full agent scheduling, UI and device validation. Legacy routes remain in source but are not mounted by the active server.
