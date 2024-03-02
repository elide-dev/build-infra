@file:Suppress("UnstableApiUsage")

pluginManagement {
  includeBuild("gradle/build-infra")

  // Install each embedded plug-in build.
  listOf(
    "base",
    "graalvm",
    "jlink",
    "jmod",
    "jpms",
    "mrjar",
  ).forEach {
    includeBuild("gradle/plugins/$it")
  }
}

plugins {
  id("infra.settings")
}

includeBuild("gradle/build-infra") {
  dependencySubstitution {
    listOf("dev.elide.infra:build-infra").forEach {
      substitute(module(it)).using(project(":"))
    }
  }
}

includeBuild("gradle/plugins/base") {
  dependencySubstitution {
    listOf("dev.elide.infra:base").forEach {
      substitute(module(it)).using(project(":"))
    }
  }
}

includeBuild("gradle/plugins/jmod") {
  dependencySubstitution {
    listOf("dev.elide.infra:jmod").forEach {
      substitute(module(it)).using(project(":"))
    }
  }
}

includeBuild("gradle/plugins/jpms") {
  dependencySubstitution {
    listOf("dev.elide.infra:jpms").forEach {
      substitute(module(it)).using(project(":"))
    }
  }
}

includeBuild("gradle/plugins/mrjar") {
  dependencySubstitution {
    listOf("dev.elide.infra:mrjar").forEach {
      substitute(module(it)).using(project(":"))
    }
  }
}

// Add samples, which test the embedded plugins.
includeBuild("gradle/samples")

rootProject.name = "build-commons"
