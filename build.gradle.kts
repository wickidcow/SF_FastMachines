import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.4.10"
    id("com.gradleup.shadow") version "9.6.1"
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://central.sonatype.com/repository/maven-snapshots/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.norain.city/snapshots")
    maven("https://repo.alessiodp.com/releases/")
    maven("https://jitpack.io")
}

val slimefunVersion = providers.gradleProperty("slimefunVersion").orElse("DEV-SNAPSHOT")

dependencies {
    fun compileOnlyAndTestImpl(dependencyNotation: Any) {
        compileOnly(dependencyNotation)
        testImplementation(dependencyNotation)
    }

    compileOnly(kotlin("stdlib"))
    compileOnly(kotlin("reflect"))
    compileOnlyAndTestImpl("io.papermc.paper:paper-api:26.2.build.+")
    compileOnlyAndTestImpl("com.github.slimefun:Slimefun:${slimefunVersion.get()}")
    compileOnly("net.guizhanss:SlimefunTranslation:e6da231617") { isTransitive = false }
    compileOnly("com.github.schntgaispock:SlimeHUD:1.3.0") { isTransitive = false }
    compileOnly("com.github.SlimefunGuguProject:InfinityExpansion:bebf0bd0f9") { isTransitive = false }
    compileOnly("com.github.VoperAD:SlimeFrame:8af2379a01") { isTransitive = false }
    compileOnly("net.guizhanss:InfinityExpansion2:8d3e6c40f6") { isTransitive = false }
    implementation("org.bstats:bstats-bukkit:3.1.0")
    implementation("net.guizhanss:guizhanlib-all:2.5.0")
    implementation("net.guizhanss:guizhanlib-kt-all:0.2.0")

    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

group = "net.guizhanss"
version = providers.gradleProperty("projectVersion").orElse("26.2-Albion-SNAPSHOT").get()
description = "FastMachines maintained for AlbionMC, Paper 26.2 and Gugu Slimefun"

val mainPackage = "net.guizhanss.fastmachines"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        javaParameters = true
        jvmTarget = JvmTarget.JVM_25
    }
}

tasks.shadowJar {
    fun doRelocate(from: String, to: String? = null) {
        val last = to ?: from.split(".").last()
        relocate(from, "$mainPackage.libs.$last")
    }

    doRelocate("net.byteflux.libby")
    doRelocate("net.guizhanss.guizhanlib")
    doRelocate("org.bstats")
    doRelocate("io.papermc.lib", "paperlib")
    archiveClassifier = ""
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}


tasks.runServer {
    downloadPlugins {
        url("https://builds.guizhanss.com/api/download/SlimefunGuguProject/Slimefun4/master/latest")
        url("https://blob.build/dl/SlimeHUD/Dev/latest")
    }
    jvmArgs("-Dcom.mojang.eula.agree=true")
    minecraftVersion("26.2")
}

tasks.test {
    useJUnitPlatform()
}
