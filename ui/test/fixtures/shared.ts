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
