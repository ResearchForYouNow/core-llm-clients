plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    `java-library`
    `maven-publish`
    id("org.jetbrains.dokka")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(project(":core-api"))
    api(project(":providers:openai"))
    api(project(":providers:gemini"))
    api(libs.bundles.ktor.client)
}
