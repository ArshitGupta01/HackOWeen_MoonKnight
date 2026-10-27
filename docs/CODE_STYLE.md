# Code style and boundaries

This repository currently uses Kotlin Multiplatform, Compose, Ktor, Koin, and SQLDelight. Browser-native web is planned but has not yet been added. Follow existing names and package conventions until a specific feature requires a change.

- Organize code by feature within the owning module. Keep domain use cases and repository ports free of UI, Ktor, SQLDelight and Koin imports.
- Keep Compose `*Route` responsible for ViewModel wiring and `*Screen` responsible for rendering state and callbacks. ViewModels call use cases only. Put data implementations behind ports.
- Keep Ktor routes thin: authenticate, validate, authorize, call a use case, and map its result to the API contract. Business transitions belong in use cases; persistence belongs in data adapters.
- Register dependencies in the existing Koin composition roots. The `app/shared` dependency on `:data` is for `di/` wiring only; review imports because Gradle cannot enforce package usage.
- Use precise names, small cohesive functions, clear error types, structured concurrency, and cancellation propagation. Explain non-obvious reasons in comments; remove dead code and debug logs.
- For the future TypeScript web app, use typed API models, accessible semantic elements, focused components, and explicit loading, empty, error, and success states. Do not mix direct database access into frontend components.
- Keep dependency versions centralized in the Gradle version catalog for Kotlin and a checked-in lockfile for web. Avoid unrelated formatting changes. Run relevant format/type checks, tests, and responsive UI review when those tools exist.

Update this guide when conventions are established in code; do not copy framework samples from the supplied beginner template.
