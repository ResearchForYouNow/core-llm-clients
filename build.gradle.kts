plugins {
    kotlin("jvm") version "2.1.21" apply false
    kotlin("plugin.serialization") version "2.1.21" apply false
    id("maven-publish")
    id("org.jetbrains.dokka") version "1.9.10" apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1" apply false
}

group = "org.researchforyounow.core"
version = "0.0.6"

repositories {
    google()
    mavenCentral()
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    // Enforce style in CI: make `check` depend on ktlintCheck when it exists
    tasks.findByName("check")?.dependsOn("ktlintCheck")
}
