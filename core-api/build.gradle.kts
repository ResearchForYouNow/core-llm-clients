import org.gradle.jvm.tasks.Jar
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    `java-library`
    `maven-publish`
    signing
    id("org.jetbrains.dokka")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(libs.slf4j.api)
    api(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.bundles.testing)
}

java {
    withSourcesJar()
}

tasks.withType<DokkaTask>().configureEach {
    outputDirectory.set(buildDir.resolve("dokka"))
}

val javadocJar by tasks.registering(Jar::class) {
    dependsOn(tasks.named("dokkaJavadoc"))
    archiveClassifier.set("javadoc")
    from(layout.buildDirectory.dir("dokka"))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "core-api"
            artifact(javadocJar)

            pom {
                name.set("LLM Clients - Core API")
                description.set("Public API for the ResearchForYouNow LLM client stack.")
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
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications["maven"])
}
