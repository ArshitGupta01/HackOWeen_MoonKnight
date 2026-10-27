import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * Data layer (Clean Architecture: adapter ring).
 *
 * - SQLite persistence through SQLDelight: ONE schema (`src/commonMain/sqldelight`) shared by
 *   Android, iOS, Desktop (JVM) and the Ktor server, so the database is identical everywhere.
 *   Only the SqlDriver differs per platform (see the `platformDataModule` actuals).
 * - Ktor client remote data source talking to the `:server` through the `:core` contract.
 * - Implements the repository interfaces declared in `:domain`.
 *
 * Web is not part of this KMP project (it is built separately with native code).
 */
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()
    jvm()
    android {
        namespace = "com.example.theekhai.data"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core"))
            implementation(project(":domain"))

            // Exposed in the public API (Koin Module, SqlDriver), hence `api`.
            api(libs.koin.core)
            api(libs.sqldelight.runtime)

            implementation(libs.sqldelight.coroutinesExtensions)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.serializationKotlinxJson)
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.androidDriver)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.nativeDriver)
            implementation(libs.ktor.client.darwin)
        }
        jvmMain.dependencies {
            implementation(libs.sqldelight.sqliteDriver)
            implementation(libs.ktor.client.okhttp)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmTest.dependencies {
            implementation(libs.sqldelight.sqliteDriver)
        }
    }
}

sqldelight {
    databases {
        create("AppDatabase") {
            packageName.set("com.example.theekhai.data.db")
        }
    }
}
