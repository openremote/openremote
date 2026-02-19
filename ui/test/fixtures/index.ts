import type {
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
export type { BasePage, ComponentTestType, Locator, Page, Project, Shared, TestFixture };
