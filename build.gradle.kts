plugins {
    java
    id("com.gradleup.shadow") version "8.3.0"
}

group = "dev.hammermaces"
version = "1.0.0"
description = "Soulbound animated maces for your SMP"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "codemc-releases"
        url = uri("https://repo.codemc.io/repository/maven-releases/")
    }
    maven {
        name = "codemc-snapshots"
        url = uri("https://repo.codemc.io/repository/maven-snapshots/")
    }
    maven {
        name = "placeholderapi"
        url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("com.github.retrooper:packetevents-spigot:2.12.0")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly(files("libs/ParticleNativeAPI.jar"))
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        archiveFileName.set("HammerMaces-${version}.jar")
    }
    build { dependsOn(shadowJar) }
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }
    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") { expand(props) }
    }
}
