/*
 * Copyright (c) 2024 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

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
