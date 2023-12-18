plugins {
    kotlin("jvm") version "1.9.21"
}

group = "dev.mtib"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    runtimeOnly("io.insert-koin:koin-core:3.5.0")
    implementation("io.insert-koin:koin-core-coroutines:3.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0-RC")

    implementation("org.slf4j:slf4j-simple:2.0.9")

    implementation("dev.kord:kord-core:0.12.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.create("fatjar", Jar::class) {
    group = "build"
    archiveFileName = "ketchup-bot-fatjar.jar"
    manifest {
        attributes["Main-Class"] = "dev.mtib.ketchup.bot.KetchupBotCommandKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets["main"].output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}

kotlin {
    jvmToolchain(8)
}