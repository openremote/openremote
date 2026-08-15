/*
 * Copyright 2026, OpenRemote Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
import { defineConfig, test as base, type PlaywrightTestConfig, type Fixtures } from "@playwright/test";
import { expect, devices } from "@playwright/experimental-ct-core";

import { defineConfig as defineCtWebConfig } from "@sand4rt/experimental-ct-web";

import { createPlugin } from "./plugin";
import { ct as ctBase, withPage, fixtures, ctFixtures } from "./fixtures";
export type * from "./fixtures";

function defineCtConfig(config: PlaywrightTestConfig): PlaywrightTestConfig {
  const original = defineCtWebConfig(config) as PlaywrightTestConfig & {
    "@playwright/test": any;
    "@playwright/experimental-ct-core": any;
  };

  return {
    ...original,
    "@playwright/test": {
      // Includes babelPlugins to transform the test source code for Playwright UI
      ...original["@playwright/test"],
      // Playwright Webpack plugin
      plugins: [createPlugin],
    },
    "@playwright/experimental-ct-core": {
      // Used to attach components to the document
      ...original["@playwright/experimental-ct-core"],
    },
  } as PlaywrightTestConfig;
}

// Must extend in the root of the package
const test = base.extend(fixtures as Fixtures);
const ct = ctBase.extend(ctFixtures as Fixtures);

export { test, ct, expect, devices, defineConfig, defineCtConfig, withPage };
