plugins {
    application
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.3.50"
}

kotlinProject()

dataLibs()

application {
    mainClassName = "io.pleo.antaeus.retrier.PleoRetrier"
}

dependencies {
    implementation(project(":pleo-bus"))

    implementation("io.javalin:javalin:2.6.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.9.8")
    compile("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.12.0")
}
