plugins {
    kotlin("jvm") version "1.8.0"
    application
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "team.redrock.rain"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.mirai.mamoe.net/snapshots")
}

dependencies {
    api(platform("net.mamoe:mirai-bom:2.15.0-dev-65"))
    api("net.mamoe:mirai-core-api")
    runtimeOnly("net.mamoe:mirai-core")

    implementation(fileTree("libs"))
    implementation("org.apache.commons:commons-email:1.5")
    implementation("com.alibaba:easyexcel:3.2.1")
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // orm
    implementation("org.jetbrains.exposed:exposed-java-time:0.38.2")
    implementation("org.jetbrains.exposed", "exposed-core", "0.37.3")
    implementation("org.jetbrains.exposed", "exposed-dao", "0.37.3")
    implementation("org.jetbrains.exposed", "exposed-jdbc", "0.37.3")
    implementation("mysql:mysql-connector-java:8.0.29")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}

application {
    mainClass.set("team.redrock.rain.MainKt")
}