import path from "node:path";

import type { i18n, Resource } from "i18next";

import type { Fixtures, PlaywrightTestArgs, Page, PlaywrightTestOptions, TestFixture } from "@playwright/test";
import { test, expect, type TestType as ComponentTestType, type Locator } from "@playwright/experimental-ct-core";

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

// type LocatorsTree<T> = {
//   [K in keyof T]: T[K] extends string ? Locator : T[K] extends Record<string, any> ? LocatorsTree<T[K]> : never;
// };

// type Tree<K extends object> = Record<keyof K, Locator>;
// type NTree<K extends object> = Record<keyof K, Locator | Tree<>>;

// type Tree<T> = {
//   [K in keyof T]: T[K] extends object ? Tree<T[K]> : Locator;
// };

type Tree<T extends Record<string, any>> = {
  [K in keyof T]: T[K] extends Record<string, any> ? Tree<T[K]> : T[K] extends string ? T[K] : Locator;
};

class Components {
  // readonly mwcInput = this.page.getByRole("");

  // readonly collapsiblePanel = this.getLocators("or-collapsible-panel", {
  //   header: "#header",
  //   content: "#content",
  // });

  // readonly jsonForms = this.page.locator("or-json-forms");

  constructor(readonly page: Page) {}

  // What about or-mwc-input? -> getInputByType

  async walkForm(locator: Locator, schema: any, path: (string | number)[] = [], item = 0, expected?: any) {
    switch (schema.type) {
      case "array": {
        locator = locator.locator("or-json-forms-array-control").nth(item);
        path.push("or-json-forms-array-control", item);
        await locator.locator("or-collapsible-panel").click();
        for (let i = 0; i < schema["or:test:item:count"]; i++) {
          await locator.getByRole("button", { name: "Add Item" }).click();
        }
        break;
      }
      case "object": {
        locator = locator.locator("or-json-forms-vertical-layout").nth(item);
        path.push("or-json-forms-vertical-layout", item);
        await locator.locator("or-collapsible-panel").click();
        for (const prop of schema["or:test:props"]) {
          await locator.getByRole("button", { name: "Add Parameter" }).click();
          const dialog = this.page.locator("or-mwc-dialog");
          await dialog
            .locator("or-mwc-list")
            .locator("li")
            .filter({ hasText: new RegExp(prop, "i") })
            .click();
          await dialog.getByRole("button", { name: "Add", exact: true }).click();
        }
        break;
      }
      case "string": {
        /// TODO: remove condition when or-json-forms-array-control always renders titles
        const options = !path.length ? { name: schema.title } : {};
        locator = locator.getByRole("textbox", options).nth(item);
        await expect(locator).toBeVisible();
        await locator.fill(schema["or:test:value"]);
        await expect(locator).toHaveValue(schema["or:test:value"]);
        break;
      }
      case "number": {
        const options = !path.length ? { name: schema.title } : {};
        locator = locator.getByRole("spinbutton", options).nth(item);
        await expect(locator).toBeVisible();
        await locator.fill(`${schema["or:test:value"]}`);
        await expect(locator).toHaveValue(`${schema["or:test:value"]}`);
        break;
      }
      case "integer": {
        const options = !path.length ? { name: schema.title } : {};
        locator = locator.getByRole("spinbutton", options).nth(item);
        await expect(locator).toBeVisible();
        await locator.fill(`${schema["or:test:value"]}`);
        await expect(locator).toHaveValue(`${schema["or:test:value"]}`);
        break;
      }
      case "boolean": {
        const options = !path.length ? { name: schema.title } : {};
        locator = locator.getByRole("checkbox", options).nth(item);
        await expect(locator).toBeVisible();
        await expect(locator).not.toBeChecked();
        if (schema["or:test:value"]) {
          await locator.check();
          await expect(locator).toBeChecked();
        }
        break;
      }
    }

    if (schema.type === "array") {
      for (let i = 0; i < schema["or:test:item:count"]; i++) {
        await this.walkForm(locator, schema.items, [...path], i);
      }
      // await locator.getByRole("button", { name: "json" }).first().click();
    } else if (schema.type === "object") {
      for (const [key, prop] of Object.entries(schema.properties)) {
        if (!schema["or:test:props"].includes(key)) continue;
        await this.walkForm(locator, prop, [...path]);
      }
      // await locator.getByRole("button", { name: "json" }).first().click();
    }

    if (schema.type === "array" && schema.type === "object") {
      // await expect(locator.locator("or-ace-editor")).toContainText(JSON.stringify(expected, null, 2));
    }
  }

  // private isLocator(value: any): value is Locator {
  //   return value && typeof value === "object" && typeof value.locator === "function";
  // }

  // private wrapLocators<T extends object>(locators: Tree<T>, root: string): Tree<T> {
  //   for (const [key, node] of Object.entries<Locator | Tree<any>>(locators)) {
  //     if (key === "name") continue;
  //     if (this.isLocator(node)) {
  //       locators[key] = this.page.locator(root).filter({ has: node });
  //     } else {
  //       locators[key] = this.wrapLocators(locators[key], root);
  //     }
  //   }
  //   return locators;
  // }

  // private getLocators<T extends string, K extends object>(
  //   root: T,
  //   internals: Tree<K> | ((name: string) => Tree<K>)
  // ): { name: T; root: Locator } & Tree<K> {
  //   if (typeof internals === "function") {
  //     internals = internals(root);
  //   }
  //   return {
  //     name: root,
  //     root: this.page.locator(root),
  //     ...(Object.fromEntries(Object.entries<string>(internals).map(([k, v]) => [k, this.page.locator(v)])) as {
  //       [V in K]: Locator;
  //     }),
  //   };
  // }

  // private wrapLocators<T extends object>(locators: T, root: string): T {
  //   const wrap = (obj: any): any => {
  //     for (const [key, value] of Object.entries(obj)) {
  //       if (key === "name") continue;

  //       if (this.isLocator(value)) {
  //         obj[key] = this.page.locator(root).filter({ has: value });
  //       } else if (value && typeof value === "object" && !Array.isArray(value)) {
  //         obj[key] = wrap(value);
  //       }
  //     }
  //     return obj;
  //   };

  //   return wrap(locators);
  // }

  // private getLocators<T extends string, R extends Record<string, any> | ((name: T) => Record<string, any>)>(
  //   root: T,
  //   internals: R
  // ): { name: T; root: Locator } & LocatorsTree<R extends (...args: any) => infer U ? U : R> {
  //   const input: Record<string, any> = typeof internals === "function" ? internals(root) : internals;

  //   const wrap = <O extends Record<string, any>>(obj: O): LocatorsTree<O> => {
  //     const result = {} as LocatorsTree<O>;

  //     for (const [key, value] of Object.entries(obj)) {
  //       if (typeof value === "string") {
  //         (result as any)[key] = this.page.locator(value);
  //       } else if (value && typeof value === "object" && !Array.isArray(value)) {
  //         (result as any)[key] = wrap(value);
  //       } else {
  //         throw new Error(`Unsupported value for locator: ${value}`);
  //       }
  //     }

  //     return result;
  //   };

  //   return {
  //     name: root,
  //     root: this.page.locator(root),
  //     ...wrap(input),
  //   };
  // }

  // private getLocators<T extends string, K extends string>(
  //   root: T,
  //   internals: Record<K, any> | ((name: string) => Record<K, any>)
  // ): { name: T; root: Locator } & {
  //   [V in K]: any;
  // } {
  //   if (typeof internals === "function") {
  //     internals = internals(root);
  //   }

  //   const wrapPaths = (obj: any): any => {
  //     const result: any = {};

  //     for (const [key, value] of Object.entries(obj)) {
  //       if (typeof value === "string") {
  //         result[key] = this.page.locator(value);
  //       } else if (value && typeof value === "object" && !Array.isArray(value)) {
  //         result[key] = wrapPaths(value);
  //       } else {
  //         result[key] = value;
  //       }
  //     }

  //     return result;
  //   };

  //   return {
  //     name: root,
  //     root: this.page.locator(root),
  //     ...wrapPaths(internals),
  //   };
  // }

  // private getLocators<T extends string, K extends string>(
  //   root: T,
  //   internals: Record<K, string> | ((name: string) => Record<K, string>)
  // ): { name: T; root: Locator } & {
  //   [V in K]: Locator;
  // } {
  //   if (typeof internals === "function") {
  //     internals = internals(root);
  //   }
  //   return {
  //     name: root,
  //     root: this.page.locator(root),
  //     ...(Object.fromEntries(Object.entries<string>(internals).map(([k, v]) => [k, this.page.locator(v)])) as {
  //       [V in K]: Locator;
  //     }),
  //   };
  // }
}

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

export const componentFixtures: Fixtures<PlaywrightTestArgs & PlaywrightTestOptions & ComponentFixtures> = {
  shared: withPage(Shared),
  // Build the component tree using nested objects internally where the keys represent the well known internals and their values the corresponding locator.
  components: withPage(Components),
};

export { test as ct, ComponentTestType };
