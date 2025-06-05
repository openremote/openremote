import type { Fixtures, PlaywrightTestArgs, Page, PlaywrightTestOptions, TestFixture } from "@playwright/test";
import { test, type TestType as ComponentTestType } from "@playwright/experimental-ct-core";

import { Components, type MountOptions, type MountResult } from "./component";
import { Shared, camelCaseToSentenceCase } from "./shared";

export interface ComponentFixtures {
  mount<HooksConfig, Component extends HTMLElement = HTMLElement>(
    component: new (...args: any[]) => Component,
    options?: MountOptions<HooksConfig, Component>
  ): Promise<MountResult<Component>>;
  shared: Shared;
  components: Components;
}

// TODO: Separate our component test fixtures from the default playwright component test fixtures
declare module "@playwright/experimental-ct-core" {
  const test: ComponentTestType<ComponentFixtures>;
}

function withPage<R>(component: Function): TestFixture<R, { page: Page }> {
  return async ({ page }, use) => await use(new (component.bind(null, page))());
}

export const fixtures: Fixtures<PlaywrightTestArgs & PlaywrightTestOptions & ComponentFixtures> = {
  shared: withPage(Shared),
  // Build the component tree using nested objects internally where the keys represent the well known internals and their values the corresponding locator.
  components: withPage(Components),
};

export { test as ct, camelCaseToSentenceCase, type ComponentTestType, type Shared, type Components };
