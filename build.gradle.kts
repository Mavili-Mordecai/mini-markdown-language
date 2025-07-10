plugins {
    kotlin("jvm") version "2.1.21"
}

group = "io.github.mavilimordecai"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.13.3")
    testImplementation("org.assertj:assertj-core:4.0.0-M1")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}