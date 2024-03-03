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

description = "Core version catalog"
group = "dev.elide.infra"

catalog {
  versionCatalog {
    description = "Core version catalog for Build Infra and downstream projects"
    from(files("../../catalogs/core.versions.toml"))
  }
}

publishing {
  publications {
    create<MavenPublication>("maven") {
      from(components["versionCatalog"])
    }
  }
}
