This is a Kotlin Multiplatform project targeting Android, iOS, Desktop (JVM), Server (web is built separately with native web code).

* [/app/iosApp](./app/iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/app/shared](./app/shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./app/shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./app/shared/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./app/shared/src/jvmMain/kotlin)
    folder is the appropriate location.

* [/core](./core/src) is for the code that will be shared between all targets in the project.
  The most important subfolder is [commonMain](./core/src/commonMain/kotlin). If preferred, you
  can add code to the platform-specific folders here too.

* [/server](./server/src/main/kotlin) is for the Ktor server application.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :app:androidApp:assembleDebug`
- Desktop app:
  - Hot reload: `./gradlew :app:desktopApp:hotRun --auto`
  - Standard run: `./gradlew :app:desktopApp:run`
- Server: `./gradlew :server:run`
- iOS app: open the [/app/iosApp](./app/iosApp) directory in Xcode and run it from there.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :app:shared:testAndroidHostTest`
- Desktop tests: `./gradlew :app:shared:jvmTest`
- Server tests: `./gradlew :server:test`
- iOS tests: `./gradlew :app:shared:iosSimulatorArm64Test`

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html) and
[Compose Multiplatform](https://kotlinlang.org/compose-multiplatform/).




---

## Architecture

This project follows Clean Architecture + MVVM with Koin DI, SQLite (SQLDelight) on Android/iOS/Desktop and a
Ktor backend. Modules: `:core` (shared contract), `:domain`, `:data`, `:app:shared`, `:server`.
See [ARCHITECTURE.md](./ARCHITECTURE.md) for the module rules, DI setup and the new-feature checklist.

Contributor guidance and final checks: [agent.md](./agent.md), [security](./docs/SECURITY.md), [code style](./docs/CODE_STYLE.md), [database](./docs/DATABASE.md), [API](./docs/API.md), and [web design](./docs/DESIGN_SYSTEM.md). The connected Supabase project is named `TheekHai`; its planned role is authentication and private evidence storage, while Ktor remains the case backend.

Extra test commands: `./gradlew :domain:jvmTest`, `./gradlew :data:jvmTest`.
