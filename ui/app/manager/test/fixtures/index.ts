import { type Page } from "@playwright/test";

export class BasePage {
  constructor(public readonly page: Page) {}

  /**
   * drag to position x and position y
   * @param x coordinate of screen pixel
   * @param y coordinate of screen pixel
   */
  async drag(x: number, y: number) {
    await this.page.mouse.down();
    await this.page.mouse.move(x, y);
    await this.page.mouse.up();
  }

  /**
   * Intercept the response of a request and handle it
   * @param url
   */
  async interceptResponse<T>(url: string, cb: (response?: T) => void) {
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
}
