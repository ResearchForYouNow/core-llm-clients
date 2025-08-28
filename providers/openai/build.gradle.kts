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
    api(project(":core-api"))

    implementation(libs.bundles.ktor.client)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.slf4j.api)

    testImplementation(libs.bundles.testing)
    testImplementation("io.ktor:ktor-client-mock:2.3.7")
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
            artifactId = "llm-provider-openai"
            artifact(javadocJar)

            pom {
                name.set("LLM Clients - OpenAI Provider")
                description.set("OpenAI provider implementation for the ResearchForYouNow LLM client stack.")
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
