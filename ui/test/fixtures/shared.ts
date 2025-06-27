import path from "node:path";

import type { i18n, Resource } from "i18next";
import type { Page } from "@playwright/test";

import { Resolve, type JsonSchema, type JsonSchema4, type CombinatorKeyword } from "@jsonforms/core";

declare global {
  interface Window {
    _i18next: i18n;
  }
}

// TODO: move to `@openremote/util`
export function camelCaseToSentenceCase(value: string) {
  const result = value
    .replace(/([A-Z])/g, " $1") // Add space before uppercase letters
    .replace(/(\d)([A-Za-z])/g, "$1 $2") // Add space between numbers and letters
    .replace(/([A-Za-z])(\d)/g, "$1 $2"); // Add space between letters and numbers
  return result.charAt(0).toUpperCase() + result.slice(1);
}

export interface BasePage {
  goto(): Promise<void>;
}

export class Shared {
  constructor(readonly page: Page) {}

  /**
   * Drag to position x and position y
   * @param x coordinate of screen pixel
   * @param y coordinate of screen pixel
   */
  async drag(x: number, y: number) {
    await this.page.mouse.down();
    await this.page.mouse.move(x, y);
    await this.page.mouse.up();
  }

  /**
   * Intercept a request and handle request body
   * @param url
   */
  async interceptRequest<T>(url: string, cb: (body?: T) => void) {
    await this.page.route(
      url,
      async (route, request) => {
        await route.continue();
        cb(await request.postDataJSON());
      },
      { times: 1 }
    );
  }

  /**
   * Intercept the response of a request and handle response body
   * @param url
   */
  async interceptResponse<T>(url: string, cb: (body?: T) => void) {
    await this.page.route(
      url,
      async (route, request) => {
        await route.continue();
        const response = await request.response();
        cb(await response?.json());
      },
      { times: 1 }
    );
  }

  async fonts() {
    await this.page.route("**/shared/fonts/**", (route, request) => {
      route.fulfill({ path: this.urlPathToFsPath(request.url()) });
    });
  }

  async locales(resources?: Resource) {
    await this.page.route("**/shared/locales/**", (route, request) => {
      route.fulfill({ path: this.urlPathToFsPath(request.url()) });
    });
    this.page.evaluate(async (resources) => {
      await window._i18next.init({
        lng: "en",
        fallbackLng: "en",
        defaultNS: "test",
        fallbackNS: "or",
        ns: ["or"],
        backend: {
          loadPath: "/shared/locales/{{lng}}/{{ns}}.json",
        },
      });
      if (resources) {
        Object.entries(resources).forEach(([locale, r]) =>
          Object.entries(r).forEach(([ns, translations]) => {
            window._i18next.addResourceBundle(locale, ns, translations);
          })
        );
      }
    }, resources);
  }

  // TODO: move to `@openremote/util`
  resolveSubSchemasRecursive(schema: JsonSchema, rootSchema: JsonSchema, keyword?: CombinatorKeyword): JsonSchema {
    const combinators: string[] = keyword ? [keyword] : ["allOf", "anyOf", "oneOf"];

    if (schema.$ref) {
      return this.resolveSubSchemasRecursive(Resolve.schema(rootSchema, schema.$ref, rootSchema), rootSchema);
    }

    combinators.forEach((combinator) => {
      const schemas = (schema as any)[combinator] as JsonSchema[];

      if (schemas) {
        (schema as any)[combinator] = schemas.map((subSchema) =>
          this.resolveSubSchemasRecursive(subSchema, rootSchema)
        );
      }
    });

    if (schema.items) {
      if (Array.isArray(schema.items)) {
        schema.items = (schema.items as JsonSchema4[]).map(
          (itemSchema) => this.resolveSubSchemasRecursive(itemSchema, rootSchema) as JsonSchema4
        );
      } else {
        schema.items = this.resolveSubSchemasRecursive(schema.items as JsonSchema, rootSchema);
      }
    }

    if (schema.properties) {
      Object.keys(schema.properties).forEach(
        (prop) => (schema.properties![prop] = this.resolveSubSchemasRecursive(schema.properties![prop], rootSchema))
      );
    }

    return schema;
  }

  private urlPathToFsPath(url: string) {
    return path.resolve(__dirname, global.decodeURI(`../../app${new URL(url).pathname}`));
  }
}
