@file:Suppress("UnstableApiUsage")

pluginManagement {
  includeBuild("../build-infra")
  includeBuild("../plugins/base")
  includeBuild("../plugins/graalvm")
  includeBuild("../plugins/jlink")
  includeBuild("../plugins/jmod")
  includeBuild("../plugins/jpms")
  includeBuild("../plugins/mrjar")
}

plugins {
  id("infra.settings")
}

include(":jmod-library")

rootProject.name = "samples"
