val ktorVersion: String by project
val kotlinVersion: String by project
val logbackVersion: String by project
val koinVersion: String by project
val exposedVersion: String by project
val sqliteVersion: String by project

plugins {
    kotlin("jvm") version "2.2.0"
    id("io.ktor.plugin") version "3.2.0"
    kotlin("plugin.serialization") version "2.2.0"
}

group = "rs.edu.raf.rma"
version = "1.0.0"

application {
    mainClass.set("io.ktor.server.cio.EngineMain")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

val generateVersionFileTask by tasks.registering {
    doLast {
        val version = project.version.toString()
        val file = File("${project.rootDir}/release.properties")
        file.createNewFile()
        file.writeText("version=${version}")
    }
}

tasks {
    create("stage") {
        dependsOn("installDist")
    }

    getByName("classes") {
        finalizedBy(generateVersionFileTask)
    }
}

repositories {
    mavenCentral()
    google()
}

dependencies {

    // Ktor Server
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-cio:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-request-validation:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")

    // Serialization
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    // DataStore
    implementation("androidx.datastore:datastore-core:1.1.1")

    // Database
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.xerial:sqlite-jdbc:$sqliteVersion")

    // DI
    implementation("io.insert-koin:koin-ktor:$koinVersion")
    implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")

    // Logging
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    // Tests
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
    testImplementation("io.kotest:kotest-assertions-core:5.5.4")
    testImplementation("io.kotest:kotest-assertions-json:5.5.4")

}
