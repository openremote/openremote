import axios, {AxiosInstance, AxiosRequestConfig, GenericAxiosResponse} from "axios";
import {ApiClient, RestResponse} from "./restclient";
import Qs from "qs";

export {RestResponse, GenericAxiosResponse};

export class RestApi {

    get api() {
        return this._client;
    }

    protected _client!: ApiClient;
    protected _axiosInstance!: AxiosInstance;

    constructor() {
        this._axiosInstance = axios.create();
        this._axiosInstance.defaults.headers["Content-Type"] = "application/json";
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
