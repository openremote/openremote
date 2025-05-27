import path from "node:path";

import { Fixtures, PlaywrightTestArgs, type Page, PlaywrightTestOptions } from "@playwright/test";
import { test, type TestType as ComponentTestType, type Locator } from "@playwright/experimental-ct-core";

type ComponentProps<Component extends HTMLElement> = Partial<Component>;
type ComponentSlot = number | string | ComponentSlot[];
type ComponentSlots = Record<string, ComponentSlot> & { default?: ComponentSlot };

type ComponentEvents = Record<string, Function>;

export interface MountOptions<HooksConfig, Component extends HTMLElement> {
  props?: ComponentProps<Component>;
  slots?: ComponentSlots;
  on?: ComponentEvents;
  hooksConfig?: HooksConfig;
}

export interface MountResult<Component extends HTMLElement> extends Locator {
  unmount(): Promise<void>;
  update(options: {
    props?: Partial<ComponentProps<Component>>;
    slots?: Partial<ComponentSlots>;
    on?: Partial<ComponentEvents>;
  }): Promise<void>;
}

class Shared {
  constructor(readonly page: Page) {}

  async fonts() {
    await this.page.route("**/shared/fonts/**", (route, request) => {
      route.fulfill({ path: path.resolve(__dirname, global.decodeURI(`../../app${new URL(request.url()).pathname}`)) });
    });
  }

  // async translations() {
  //   await this.page.route("**/shared/**", (route, request) => {
  //     console.trace(request.url());
  //     route.continue();
  //   });
  // }
}

export interface ComponentFixtures {
  mount<HooksConfig, Component extends HTMLElement = HTMLElement>(
    component: new (...args: any[]) => Component,
    options?: MountOptions<HooksConfig, Component>
  ): Promise<MountResult<Component>>;
  shared: Shared;
}

// TODO: Separate our component test fixtures from the default playwright component test fixtures
declare module "@playwright/experimental-ct-core" {
  const test: ComponentTestType<ComponentFixtures>;
}

export const componentFixtures: Fixtures<PlaywrightTestArgs & PlaywrightTestOptions & ComponentFixtures> = {
  shared: async ({ page }, use) => await use(new Shared(page)),
};

export { test as ct, ComponentTestType };
