plugins {
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.serialization") version "1.9.21"
}

group = "dev.mtib"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    runtimeOnly("io.insert-koin:koin-core:3.5.0")
    implementation("io.insert-koin:koin-core-coroutines:3.5.0")
    runtimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    implementation("org.slf4j:slf4j-simple:2.0.9")

    implementation("dev.kord:kord-core:0.12.0")

    val exposedVersion = "0.47.0"
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-crypt:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")

    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-json:$exposedVersion")

    implementation("org.xerial:sqlite-jdbc:3.45.1.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.+")

    implementation(platform("com.aallam.openai:openai-client-bom:3.7.1"))

    implementation("com.aallam.openai:openai-client")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    runtimeOnly("io.ktor:ktor-client-okhttp")

    implementation("io.arrow-kt:arrow-core:1.2.4")
    implementation("io.arrow-kt:arrow-fx-coroutines:1.2.4")

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