plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("examples.ConsumerReadyExample")
}

dependencies {
    implementation(project(":core"))

    implementation(libs.bundles.ktor.client)
    implementation(libs.kotlinx.serialization.json)
    runtimeOnly("org.slf4j:slf4j-simple:2.0.13")
}
