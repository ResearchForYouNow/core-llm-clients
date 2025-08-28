plugins {
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    kotlin("jvm") version "2.1.21" apply false
    kotlin("plugin.serialization") version "2.1.21" apply false
    `java-library`
    id("maven-publish")
    id("signing")
    id("org.jetbrains.dokka") version "1.9.10" apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1" apply false
}

allprojects {
    group = "io.github.researchforyounow"
    version = "0.6.0"
}

group = "io.github.researchforyounow"
version = "0.6.0"

repositories {
    google()
    mavenCentral()
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
            stagingProfileId.set("io.github.researchforyounow")
            username.set(providers.gradleProperty("OSSRH_USERNAME"))
            password.set(providers.gradleProperty("OSSRH_PASSWORD"))
        }
    }
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    tasks.findByName("check")?.dependsOn("ktlintCheck")
}
