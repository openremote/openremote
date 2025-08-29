import path from "node:path";

import type { i18n, Resource } from "i18next";
import type { Page } from "@playwright/test";

declare global {
  interface Window {
    _i18next: i18n;
  }
}

export interface BasePage {
  goto(): Promise<void>;
}

export class Shared {
  constructor(readonly page: Page) {}

  /**
   * Drag to position x and position y
   * @param x coordinate of screen in pixels
   * @param y coordinate of screen in pixels
   */
  async drag(x: number, y: number) {
    await this.page.mouse.down();
    await this.page.mouse.move(x, y);
    await this.page.mouse.up();
  }

  /**
   * Intercept a request and handle the request body.
   * @param url The URL to intercept
   * @param cb The callback to handle the request
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
   * Intercept the response of a request and handle the response body.
   * @param url The URL to intercept
   * @param cb The callback to handle the response
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

  /**
   * Init shared fonts to be served for material design icons.
   */
  async fonts() {
    await this.page.route("**/shared/fonts/**", (route, request) => {
      route.fulfill({ path: this.urlPathToFsPath(request.url()) });
    });
  }

  /**
   * Init shared translations to be served for i18next.
   * @param resources The custom translations to add
   */
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

  /**
   * Resolves a request URL to a local filesystem path.
   * @param url The icoming request URL to resolve
   */
  private urlPathToFsPath(url: string) {
    return path.resolve(__dirname, decodeURI(`../../app${new URL(url).pathname}`));
  }
}
