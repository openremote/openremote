/*
 * Copyright 2026, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
import { AxiosError, type AxiosAdapter, type AxiosResponse, type InternalAxiosRequestConfig } from "axios";
import type { APIRequestContext, APIResponse } from "@openremote/test";

/**
 * Builds the absolute request URL (baseURL + url + serialized query params) the way axios' default adapter would,
 * reusing the request's paramsSerializer so array params keep the same repeat format as the app's rest client.
 */
function buildUrl(config: InternalAxiosRequestConfig): string {
    const base = (config.baseURL ?? "").replace(/\/+$/, "");
    const path = config.url ?? "";
    const url = /^https?:\/\//i.test(path) ? path : `${base}/${path.replace(/^\/+/, "")}`;

    if (!config.params) {
        return url;
    }
    const serialize = typeof config.paramsSerializer === "function"
        ? config.paramsSerializer
        : (params: Record<string, unknown>) => new URLSearchParams(params as Record<string, string>).toString();
    const query = serialize(config.params);
    return query ? `${url}${url.includes("?") ? "&" : "?"}${query}` : url;
}

/** Flattens axios' (possibly AxiosHeaders) headers into a plain string map for Playwright's request. */
function toHeaders(config: InternalAxiosRequestConfig): Record<string, string> {
    const raw: Record<string, unknown> =
        config.headers && typeof (config.headers as { toJSON?: () => unknown }).toJSON === "function"
            ? (config.headers as { toJSON: () => Record<string, unknown> }).toJSON()
            : ((config.headers as Record<string, unknown>) ?? {});
    const headers: Record<string, string> = {};
    for (const [key, value] of Object.entries(raw)) {
        if (value == null) continue;
        headers[key] = Array.isArray(value) ? value.join(", ") : String(value);
    }
    return headers;
}

/** Decodes a Playwright response body into the value axios would expose as {@link AxiosResponse.data}. */
async function decodeBody(response: APIResponse, config: InternalAxiosRequestConfig): Promise<unknown> {
    const responseType = config.responseType ?? "json";
    if (responseType === "arraybuffer") {
        return response.body();
    }
    const text = await response.text();
    if (!text) {
        return undefined;
    }
    const isJson = responseType === "json"
        && (response.headers()["content-type"] ?? "").toLowerCase().includes("application/json");
    return isJson ? JSON.parse(text) : text;
}

/**
 * An axios adapter that dispatches every request through Playwright's {@link APIRequestContext} (`request`) instead
 * of Node's http stack. This keeps the generated `@openremote/rest` typed client while ensuring the REST calls made
 * during test setup are captured in the Playwright trace viewer.
 *
 * axios interceptors and `transformRequest` have already run by the time the adapter is invoked, so the body is a
 * serialized string (or undefined) and the params serializer is set; this only translates the final config into a
 * `request.fetch` call and the response back into an {@link AxiosResponse} (throwing an {@link AxiosError} on a
 * status the request's `validateStatus` rejects, to match the default adapter).
 */
export function playwrightRequestAdapter(request: APIRequestContext): AxiosAdapter {
    return async (config: InternalAxiosRequestConfig): Promise<AxiosResponse> => {
        const response = await request.fetch(buildUrl(config), {
            method: (config.method ?? "get").toUpperCase(),
            headers: toHeaders(config),
            ...(config.data != null && { data: config.data }),
            ...(config.timeout && { timeout: config.timeout }),
            ...(config.maxRedirects != null && { maxRedirects: config.maxRedirects }),
            // Never throw on status; validateStatus is applied below to mirror axios
            failOnStatusCode: false,
        });

        const axiosResponse: AxiosResponse = {
            data: await decodeBody(response, config),
            status: response.status(),
            statusText: response.statusText(),
            headers: response.headers(),
            config,
            request: undefined,
        };

        const validateStatus = config.validateStatus ?? ((status: number) => status >= 200 && status < 300);
        if (!validateStatus(axiosResponse.status)) {
            throw new AxiosError(
                `Request failed with status code ${axiosResponse.status}`,
                axiosResponse.status >= 500 ? AxiosError.ERR_BAD_RESPONSE : AxiosError.ERR_BAD_REQUEST,
                config,
                undefined,
                axiosResponse,
            );
        }
        return axiosResponse;
    };
}
