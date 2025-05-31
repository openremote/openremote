import path from "node:path";

import type { i18n, Resource } from "i18next";

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

declare global {
  interface Window {
    _i18next: i18n;
  }
}

class Shared {
  constructor(readonly page: Page) {}

  async fonts() {
    await this.page.route("**/shared/fonts/**", (route, request) => {
      route.fulfill({ path: this.urlPathToFsPath(request.url()) });
    });
  }

  async locales(resources?: Resource) {
    await this.page.route("**/shared/locales/**", (route, request) => {
      route.fulfill({ path: this.urlPathToFsPath(request.url()) });
    });
    this.page.evaluate((resources) => {
      window._i18next.init({
        lng: "en",
        fallbackLng: "en",
        defaultNS: "test",
        fallbackNS: "or",
        ns: ["or"],
        backend: {
          loadPath: "/shared/locales/{{lng}}/{{ns}}.json",
        },
        resources,
      });
    }, resources);
  }

  private urlPathToFsPath(url: string) {
    return path.resolve(__dirname, global.decodeURI(`../../app${new URL(url).pathname}`));
  }
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
