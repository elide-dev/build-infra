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

/**
 *
 * Elide Build Infrastructure for Gradle
 * -------------------------------------
 *
 * This script is designed to be applied directly to a Gradle build, in the form of a `apply(from = ...)`
 * directive within a `settings.gradle.kts` or `settings.gradle` file. Using this script won't work from
 * a regular build script.
 *
 * The centralized Gradle build infrastructure installs the following in a Gradle build:
 * - A common suite of settings-time plugins (Gradle Enterprise, Foojay, Common User Data, etc.)
 * - A common suite of build convention plugins (within the `elide.build.*` namespace, see repo for more)
 * - A suite of tailored repository settings, via Pkgst (https://pkg.st)
 *
 */
