# Architecture

Clean Architecture + MVVM, Kotlin Multiplatform, Koin for dependency injection, one SQLite schema
(SQLDelight) on Android / iOS / Desktop, and a Ktor backend that shares contracts with the apps.

## Module graph (dependencies point inward only)

```
 :app:androidApp  :app:desktopApp  :app:iosApp   (platform entry points)
              \        |            /           /
               ---->  :app:shared  <------------                (presentation: MVVM + Koin root)
                       |      \
                       |       `---------------------------> :data   (SQLDelight, Ktor client, repo impls)
                       v                                      |
                    :domain  <--------------------------------'    (entities, ports, use cases)
                       ^                          :server ---> :data (jvm) , :domain
                       |
                    :core  (shared contract: DTOs, ApiRoutes, utils)  <-- used by every module above
```

| Module | Layer | May depend on | Must NOT depend on |
|---|---|---|---|
| `:core` | Shared contract (`GreetingDto`, `ApiRoutes`) | kotlinx.serialization | everything else |
| `:domain` | Entities, repository interfaces, use cases | coroutines | `:data`, UI, Ktor, SQLDelight, Koin |
| `:data` | Repository implementations, SQLite, Ktor client | `:domain`, `:core` | `:app:*`, Compose |
| `:app:shared` | ViewModels, Compose UI, Koin composition root | `:domain`, `:core`, `:data` (wiring only, in `di/`) | - |
| `:app:*App` | Platform entry points | `:app:shared` | `:data` directly |
| `:server` | Ktor routes + Koin | `:domain`, `:data`, `:core` | `:app:*` |

Gradle enforces these rules: a forbidden import will not compile because the dependency is not declared.

## MVVM (presentation)

`Screen (Composable)` observes `StateFlow<UiState>` from a `ViewModel`, which calls **use cases only**.
Screens are split into a stateful `*Route` (gets the ViewModel through `koinViewModel()`) and a stateless
`*Screen` (previewable, testable). See `presentation/greeting`.

## Dependency injection (Koin)

- `:domain` has no DI dependency; use cases are registered in `app/shared/.../di/AppModule.kt` (`domainModule`).
- `:data` exposes `dataModule`, `localDataModule`, `remoteDataModule` and an `expect val platformDataModule`
  (actuals provide the `SqlDriver` and the backend URL per platform).
- `initKoin { ... }` is the single entry point: Android (`TheekHaiApplication`), Desktop (`main.kt`),
  iOS (`startKoinForIos()` from `iOSApp.swift`).
- The server uses `koin-ktor` (`serverModule` in `server/.../di`).

## Database (SQLite everywhere)

- Schema: `data/src/commonMain/sqldelight/.../Greeting.sq` - the only place tables are defined.
- Drivers: Android `AndroidSqliteDriver`, iOS `NativeSqliteDriver`, Desktop + Server `JdbcSqliteDriver`.
- Schema changes: bump by adding `data/src/commonMain/sqldelight/migrations/<n>.sqm`.
- Web is **not** part of this KMP project: it is built separately with native web code and consumes the Ktor
  REST API described by `:core` (`ApiRoutes`, `GreetingDto`). It never touches the SQLite layer.

## Common backend

- `:core` holds `GreetingDto` and `ApiRoutes`; server and clients compile against the same types.
- The server runs the same `:domain` use cases and `:data` SQLite code as the apps.
- Clients are offline-first: SQLite is the source of truth, the backend call falls back to local generation.
- Dev URLs: Android emulator `http://10.0.2.2:8080`, iOS simulator / Desktop `http://localhost:8080`.
  Override the server DB file with the `THEEKHAI_DB_PATH` environment variable.

## Adding a feature (checklist)

1. `:core` - add DTO / route constants if it touches the backend.
2. `:domain` - entity, repository interface, use case (+ unit test with a fake).
3. `:data` - `.sq` queries, data sources, repository implementation, register in `dataModule`.
4. `:app:shared` - UiState, ViewModel (`viewModelOf`), Route + Screen, register in `presentationModule`/`domainModule`.
5. `:server` - route calling the use case, register in `serverModule`.
