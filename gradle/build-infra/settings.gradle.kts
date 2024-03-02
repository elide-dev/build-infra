@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        maven {
            name = "pkgst-gradle"
            url = uri("https://gradle.pkg.st")
        }
        maven {
            name = "pkgst-maven"
            url = uri("https://maven.pkg.st")
        }
        mavenCentral {
            name = "maven-central"
        }
        gradlePluginPortal {
            name = "gradle-plugins"
        }
    }
}

plugins {
  id("com.gradle.enterprise") version ("3.16.2")
}

dependencyResolutionManagement {
    rulesMode = RulesMode.PREFER_SETTINGS
    repositoriesMode = RepositoriesMode.PREFER_SETTINGS

    repositories {
        maven {
            name = "pkgst-maven"
            url = uri("https://maven.pkg.st")
        }
        maven {
            name = "pkgst-gradle"
            url = uri("https://gradle.pkg.st")
        }
        mavenCentral {
            name = "maven-central"
        }
        gradlePluginPortal {
            name = "gradle-plugins"
        }
    }

    versionCatalogs {
        create("libs") {
            from(files("../catalogs/libs.versions.toml"))
        }
        create("core") {
            from(files("../catalogs/core.versions.toml"))
        }
        create("infra") {
            from(files("../catalogs/infra.versions.toml"))
        }
    }
}

rootProject.name = "build-infra"

buildCache {
    local {
        isEnabled = true
        removeUnusedEntriesAfterDays = 14
        directory = file(".codebase/build-cache")
    }
}

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
