import axios, {AxiosInstance, AxiosRequestConfig, GenericAxiosResponse, AxiosError} from "axios";
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

    public setTimeout(timeout: number) {
        this._axiosInstance.defaults.timeout = timeout;
    }

    public addRequestInterceptor(interceptor: (config: AxiosRequestConfig) => AxiosRequestConfig) {
        this._axiosInstance.interceptors.request.use(interceptor);
    }

    public initialise(baseUrl: string) {
        this._client = new ApiClient(baseUrl, this._axiosInstance);
    }
}

export default new RestApi();
