# Database and storage

## Current implementation

`data/src/commonMain/sqldelight` defines the shared SQLDelight schema. `Greeting.sq` is the only current table/query file. Android uses `AndroidSqliteDriver`, iOS uses `NativeSqliteDriver`, and Desktop/Server use `JdbcSqliteDriver`. `THEEKHAI_DB_PATH` selects the server database file; in-memory SQLite is used for tests.

Each app and the server has a separate database instance. The server database is authoritative for shared cases and workflow. Native SQLite is for local observation, cache, and drafts; it cannot finalize assignments or closures. Browser web will use Ktor's API and no local SQLDelight database.

The connected Supabase project `TheekHai` (`ffmcurazxvygsifbglwy`) currently has no application tables in `public`. The planned use is Auth and private evidence Storage. Do not create duplicate case tables there merely to make web access easier.

## Changes and migration rules

- Define case, report, department, task, evidence metadata, deadline, and append-only audit tables only when implementing the workflow. Use stable IDs, foreign keys, useful constraints and indexes, and UTC timestamps.
- Add SQLDelight migrations under `data/src/commonMain/sqldelight/migrations/`. Test a database created by the previous schema version and the fresh-install path.
- Group status changes, task effects, and audit events in a transaction. Deduplicate retryable requests with an idempotency key and escalation events with a database uniqueness rule.
- Keep uploaded bytes in private evidence storage, with only references and ownership metadata in the authoritative case database.
- Use synthetic development fixtures. Never reset or overwrite a production database to bypass migrations. Review a backup and restore path before production release.

If Supabase application tables are ever added, record their ownership purpose here, use versioned migrations, enable RLS on exposed tables, and test policies. Supabase Auth-managed tables are provider-owned.
