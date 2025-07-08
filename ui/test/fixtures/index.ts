import type { Fixtures, PlaywrightTestArgs, Page, PlaywrightTestOptions, TestFixture } from "@playwright/test";
import { test, type TestType as ComponentTestType } from "@playwright/experimental-ct-core";

import { type MountOptions, type MountResult } from "./component";
import { Shared, type BasePage } from "./shared";

export interface ComponentTestFixtures {
  mount<HooksConfig, Component extends HTMLElement = HTMLElement>(
    component: new (...args: any[]) => Component,
    options?: MountOptions<HooksConfig, Component>
  ): Promise<MountResult<Component>>;
  shared: Shared;
}

// TODO: Separate our component test fixtures from the default playwright component test fixtures
declare module "@playwright/experimental-ct-core" {
  const test: ComponentTestType<ComponentTestFixtures>;
}

export function withPage<R>(component: Function): TestFixture<R, { page: Page }> {
  return async ({ page }, use) => await use(new (component.bind(null, page))());
}

export const fixtures: Fixtures<PlaywrightTestArgs & PlaywrightTestOptions & ComponentTestFixtures> = {
  shared: withPage(Shared),
};

export { test as ct, type ComponentTestType, type Shared, type BasePage, type Page };
