import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.4.20"
    id("com.gradleup.shadow") version "9.3.1" // нужен Gradle 9+ и JDK 17+ для самой сборки
}

group = "dev.fmsubscribe"
version = "2.0.0"

repositories {
    mavenCentral()
    maven("https://repo.purpurmc.org/snapshots")            // новый репозиторий Purpur
    maven("https://repo.papermc.io/repository/maven-public/") // транзитивные зависимости (bungee chat и т.д.)
    maven("https://repo.helpch.at/releases/")                  // PlaceholderAPI
}

dependencies {
    compileOnly("org.purpurmc.purpur:purpur-api:1.16.5-R0.1-SNAPSHOT")
    compileOnly("net.luckperms:api:5.5")          // плагин LuckPerms 5.5.x использует API 5.5
    compileOnly("me.clip:placeholderapi:2.12.3")
    implementation(kotlin("stdlib"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8) // сервер 1.16.5 может работать на Java 8+
    }
}

tasks.processResources {
    filesMatching("plugin.yml") { expand("version" to project.version) }
}

tasks.shadowJar {
    archiveClassifier.set("")
    // Kotlin stdlib кладём внутрь jar и переименовываем, чтобы не конфликтовать с другими плагинами
    relocate("kotlin", "dev.fmsubscribe.libs.kotlin")
    exclude("org/jetbrains/annotations/**", "DebugProbesKt.bin")
}

tasks.jar { enabled = false }
tasks.build { dependsOn(tasks.shadowJar) }
