import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * Domain layer (Clean Architecture: innermost ring).
 *
 * Pure Kotlin: entities, repository *interfaces* (ports) and use cases.
 * RULE: this module must never depend on `:data`, UI, Compose, Ktor, SQLDelight or Koin.
 */
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    jvm()
    android {
        namespace = "com.example.theekhai.domain"
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
            api(project(":core"))
            api(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
