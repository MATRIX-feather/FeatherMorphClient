pluginManagement {
    repositories {
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }

        maven {
            url = uri("https://repo.papermc.io/repository/maven-public/")
        }

        gradlePluginPortal()
    }
}

plugins {
    // See https://github.com/jpenilla/run-task/wiki/Debugging#hot-swap
    // add toolchain resolver
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}