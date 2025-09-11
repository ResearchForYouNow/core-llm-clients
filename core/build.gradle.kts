import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.tasks.Jar
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    `maven-publish`
    signing
    id("org.jetbrains.dokka")
}

kotlin {
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation("io.ktor:ktor-client-core:3.2.3")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.bundles.testing)
                implementation("io.ktor:ktor-client-mock:3.2.3")
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-okhttp:3.2.3")
                implementation("org.slf4j:slf4j-api:2.0.16")
            }
        }
        val jvmTest by getting
    }
    withSourcesJar()
}

tasks.withType<DokkaTask>().configureEach {
    outputDirectory.set(layout.buildDirectory.dir("dokka"))
}

val javadocJar by tasks.registering(Jar::class) {
    dependsOn(tasks.named("dokkaHtml"))
    archiveClassifier.set("javadoc")
    from(layout.buildDirectory.dir("dokka"))
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("LLM Clients")
            description.set("Complete LLM clients library with unified interface for various LLM providers.")
            url.set("https://github.com/ResearchForYouNow/core-llm-clients")
            licenses {
                license {
                    name.set("Apache License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            developers {
                developer {
                    id.set("researchforyounow")
                    name.set("Research For You Now")
                    email.set("researchforyounow@gmail.com")
                }
            }
            scm {
                url.set("https://github.com/ResearchForYouNow/core-llm-clients")
                connection.set("scm:git:https://github.com/ResearchForYouNow/core-llm-clients.git")
                developerConnection.set("scm:git:ssh://git@github.com/ResearchForYouNow/core-llm-clients.git")
            }
        }

        // attach Dokka JAR to all publications (kotlinMultiplatform, jvm, etc.)
        artifact(javadocJar.get())
    }

    publications.named<MavenPublication>("kotlinMultiplatform") {
        artifactId = "llm-clients"
    }
    publications.named<MavenPublication>("jvm") {
        artifactId = "llm-clients-jvm"
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications)
}

// Make sure signatures exist before any publish-to-sonatype
tasks.withType<PublishToMavenRepository>().configureEach {
    dependsOn(tasks.withType<Sign>())
}
