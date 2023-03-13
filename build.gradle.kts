plugins {
    kotlin("jvm") version "1.8.0"
    application
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "team.redrock.rain"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    api(platform("net.mamoe:mirai-bom:2.14.0"))
    api("net.mamoe:mirai-core-api")
    runtimeOnly("net.mamoe:mirai-core")

    implementation("org.apache.commons:commons-email:1.5")
    implementation("com.alibaba:easyexcel:3.2.1")
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.google.code.gson:gson:2.10.1")

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