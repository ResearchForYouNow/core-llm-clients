import java.util.Properties
import kotlin.apply

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
    api(libs.slf4j.api)
    api(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.bundles.testing)
}

val keys = project.rootProject
    .file("keys.properties")
    .inputStream()
    .use {
        Properties().apply { load(it) }
    }

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "org.researchforyounow.core"
            artifactId = "llm-clients"
            version = "0.0.6"

            from(components["java"])

            // Optional: Add POM information
            pom {
                name.set("Llm clients")
                description.set("A LLM clients core library for Research For You Now organization")
                url.set("https://github.com/researchforyounow/core-llm-clients")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/researchforyounow/core-llm-clients")
            credentials {
                username = keys["GITHUB_USERNAME"] as? String
                    ?: throw GradleException("GitHub username not found.")
                password = keys["GITHUB_TOKEN"] as? String
                    ?: throw GradleException("GitHub token not found.")
            }
        }
    }
}
