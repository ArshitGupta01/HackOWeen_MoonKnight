plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
}

group = "com.example.theekhai"
version = "1.0.0"
application {
    mainClass = "com.example.theekhai.ApplicationKt"
}

dependencies {
    // Same layers as the apps: the backend reuses :domain use cases and the :data SQLite schema.
    api(project(":core"))
    implementation(project(":domain"))
    implementation(project(":data"))

    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.serverContentNegotiation)
    implementation(libs.ktor.serverCors)
    implementation(libs.ktor.serializationKotlinxJson)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.koin.core)
    implementation(libs.koin.ktor)
    implementation(libs.koin.loggerSlf4j)
    implementation(libs.sqldelight.runtime)
    implementation(libs.sqldelight.sqliteDriver)

    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
}
