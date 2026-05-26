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
import axios, {AxiosInstance, GenericAxiosResponse, AxiosError, InternalAxiosRequestConfig} from "axios";
import {ApiClient, RestResponse} from "./restclient";
import Qs from "qs";

const isAxiosError = axios.isAxiosError;

export {RestResponse, GenericAxiosResponse, AxiosError, isAxiosError};

export class RestApi {

    get api() {
        return this._client;
    }

    protected _client!: ApiClient;
    protected _axiosInstance!: AxiosInstance;
    protected _baseUrl!: string;

    constructor() {
        this._axiosInstance = axios.create();
        this._axiosInstance.defaults.headers.common["Content-Type"] = "application/json";
        this._axiosInstance.interceptors.request.use((config) => {
            config.paramsSerializer = (params) => Qs.stringify(params, {arrayFormat: "repeat"});
            return config;
        });
    }

    get axiosInstance() {
        return this._axiosInstance;
    }

    get baseUrl() {
        return this._baseUrl;
    }

    public setTimeout(timeout: number) {
        this._axiosInstance.defaults.timeout = timeout;
    }

    public addRequestInterceptor(interceptor: (config: InternalAxiosRequestConfig) => InternalAxiosRequestConfig) {
        this._axiosInstance.interceptors.request.use(interceptor);
    }

    public initialise(baseUrl: string) {
        this._baseUrl = baseUrl;
        this._client = new ApiClient(baseUrl, this._axiosInstance);
    }
}

export default new RestApi();
