plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
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
    implementation("io.github.oshai:kotlin-logging:7.0.0")

    implementation("com.mohamedrejeb.ksoup:ksoup-html:0.4.1")

    implementation("dev.kord:kord-core:0.14.0")

    val exposedVersion = "0.47.0"
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-crypt:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")

    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-json:$exposedVersion")

    implementation("org.xerial:sqlite-jdbc:3.45.1.0")

    val jacksonVersion = "2.18.2"
    implementation("com.fasterxml.jackson:jackson-bom:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")


    implementation(platform("com.aallam.openai:openai-client-bom:3.8.2"))
    implementation("com.aallam.openai:openai-client")
    runtimeOnly("io.ktor:ktor-client-okhttp")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("io.arrow-kt:arrow-core:1.2.4")
    implementation("io.arrow-kt:arrow-fx-coroutines:1.2.4")

    // Lets-Plot Kotlin API
    implementation("org.jetbrains.lets-plot:lets-plot-kotlin-jvm:4.9.2")
    implementation("org.jetbrains.lets-plot:lets-plot-jfx:2.0.2")
    runtimeOnly("org.jetbrains.lets-plot:lets-plot-image-export:4.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.11.0")



    testImplementation(kotlin("test"))

    val kotestVersion = "5.9.0"
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-property:$kotestVersion")
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
    jvmToolchain(23)
}