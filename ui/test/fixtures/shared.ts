/*
 * Copyright 2026, OpenRemote Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
import type { Page, Request, Response } from "@playwright/test";

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
  async interceptResponse<T>(url: string, cb: (body?: T, request?: Request, response?: Response | null) => void) {
    await this.page.route(
      url,
      async (route, request) => {
        await route.continue();
        const response = await request.response();
        cb(await response?.json(), request, response);
      },
      { times: 1 }
    );
  }
}
