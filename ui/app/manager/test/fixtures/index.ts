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
}
