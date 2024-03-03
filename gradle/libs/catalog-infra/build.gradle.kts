import dev.elide.infra.gradle.catalogs.VersionCatalogMergeTask

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

plugins {
  `version-catalog`
  `maven-publish`

  id("infra.catalog")
}

description = "Infra version catalog"
group = "dev.elide.infra"

infraCatalog {
  catalogs.from(
    "../../catalogs/core.versions.toml",
    "../../catalogs/infra.versions.toml",
  )
}

catalog {
  versionCatalog {
    description = "Vetted and security-hardened build tooling for Gradle"
    from(files("mergedCatalog/catalog.versions.toml"))
  }
}

publishing {
  publications {
    create<MavenPublication>("maven") {
      from(components["versionCatalog"])
    }
  }
}
