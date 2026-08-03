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
import type {
  APIRequestContext,
  APIResponse,
  Fixtures,
  PlaywrightTestArgs,
  Page,
  PlaywrightTestOptions,
  TestFixture,
  Locator,
  Project,
} from "@playwright/test";
import { test, type TestType as ComponentTestType } from "@playwright/experimental-ct-core";

import { CtShared, type MountOptions, type MountResult } from "./component";
import { Shared, type BasePage } from "./shared";

export interface SharedAppTestFixtures {
  shared: Shared;
}

export interface SharedComponentTestFixtures {
  mount<HooksConfig, Component extends HTMLElement = HTMLElement>(
    component: new (...args: any[]) => Component,
    options?: MountOptions<HooksConfig, Component>
  ): Promise<MountResult<Component>>;
  shared: CtShared;
}

declare module "@playwright/experimental-ct-core" {
  const test: ComponentTestType<SharedComponentTestFixtures>;
}

export function withPage<R>(fixture: Function): TestFixture<R, { page: Page }> {
  return async ({ page }, use) => await use(new (fixture.bind(null, page))());
}

export const fixtures: Fixtures<PlaywrightTestArgs & PlaywrightTestOptions & SharedAppTestFixtures> = {
  shared: withPage(Shared),
};

export const ctFixtures: Fixtures<PlaywrightTestArgs & PlaywrightTestOptions & SharedComponentTestFixtures> = {
  shared: withPage(CtShared),
};

export const ct = test;
export type {
  APIRequestContext,
  APIResponse,
  BasePage,
  ComponentTestType,
  Locator,
  Page,
  Project,
  Shared,
  TestFixture,
};
