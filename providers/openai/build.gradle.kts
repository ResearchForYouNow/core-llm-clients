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

    implementation(libs.bundles.ktor.client)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.slf4j.api)

    testImplementation(libs.bundles.testing)
    testImplementation("io.ktor:ktor-client-mock:2.3.7")
}
