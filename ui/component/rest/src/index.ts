import openremote, {Auth, OREvent} from "@openremote/core";
import axios, {AxiosInstance} from "axios";
import {ApiClient} from "./restclient";
import {Promise} from "es6-promise";

class RestApi {

    get api() {
        return this._client;
    }

    protected static getBaseUrl(): string {
        let baseUrl = openremote.config.managerUrl;
        baseUrl += "/api/" + openremote.config.realm + "/";
        return baseUrl;
    }

    protected _client!: ApiClient;
    protected _axiosInstance!: AxiosInstance;

    constructor() {
        openremote.addListener((evt) => this.onEvent(evt));
        this.initClient();
    }

    protected onEvent(event: OREvent) {
        switch (event) {
            case OREvent.AUTHENTICATED:
                break;
            case OREvent.ERROR:
                break;
            case OREvent.INIT:
                this.initClient();
                break;
        }
    }

    protected initClient() {
        if (!this._client && openremote.initialised && openremote.isManagerAvailable) {
            this._axiosInstance = axios.create();
            this._axiosInstance.defaults.timeout = 10000;

            if (openremote.config.auth === Auth.BASIC && openremote.config.credentials) {
                this._axiosInstance.defaults.auth = {
                    password: openremote.config.credentials.password,
                    username: openremote.config.credentials.username
                };
            } else if (openremote.config.auth === Auth.KEYCLOAK) {
                // Add interceptor to inject authorization header on each request
                this._axiosInstance.interceptors.request.use(
                    (config) => {
                        if (!config.headers.Authorization) {
                            const token = openremote.getKeycloakToken();

                            if (token) {
                                config.headers.Authorization = "Bearer " + token;
                            }
                        }

                        return config;
                    },
                    (error) => Promise.reject(error)
                );
            }
            this._client = new ApiClient(RestApi.getBaseUrl(), this._axiosInstance);
        }
    }
}

export default new RestApi();
