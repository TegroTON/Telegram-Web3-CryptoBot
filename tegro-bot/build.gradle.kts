import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

dependencies {
    val vkSdkKotlinVersion = "0.0.8"
    val ydbVersion = "2.0.1"
    val ktorVersion = "2.3.3"
    val tonKotlinVersion = "0.3.0-SNAPSHOT"

    implementation("com.petersamokhin.vksdk:core:$vkSdkKotlinVersion")

    // If your project is not JVM-based, or you simply want to use ktor.
    implementation("com.petersamokhin.vksdk:http-client-jvm-okhttp:$vkSdkKotlinVersion")

    // In this case, `ktor-client` is required. You can use any.
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-cio:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("org.jetbrains.kotlinx:atomicfu:0.21.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.4")
    implementation("org.jetbrains.exposed:exposed-core:0.41.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.41.1")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:0.58.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
    implementation("io.github.g0dkar:qrcode-kotlin-jvm:3.3.0")
    implementation("net.dzikoysk:exposed-upsert:1.1.0")
    implementation("org.postgresql:postgresql:42.6.0")
    implementation("org.telegram:telegrambots:6.5.0")
    implementation("tech.ydb:ydb-sdk-core:$ydbVersion")
    implementation("tech.ydb:ydb-sdk-table:$ydbVersion")
    implementation("tech.ydb.auth:yc-auth-provider:$ydbVersion")
    implementation("org.ton:ton-kotlin-jvm:$tonKotlinVersion")
    implementation("com.github.bnb-chain:java-sdk:1.1.5")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.5")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-properties:1.5.0")
    implementation("fr.minuskube:jpastee:1.0.2")
    implementation("ch.qos.logback:logback-classic:1.4.6")
    implementation("org.web3j:core:5.0.0")
    implementation("net.logicsquad:nanocaptcha:1.5")
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "18"
    targetCompatibility = "18"
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "18"
    }
}

application {
    mainClass.set("money.tegro.bot.LauncherKt")
}

//kotlin {
//    sourceSets.all {
//        languageSettings {
//            languageVersion = "2.0"
//        }
//    }
//}
