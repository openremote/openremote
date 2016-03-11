package org.openremote.manager.shared.rest;

import org.openremote.manager.shared.Consumer;
import org.openremote.manager.shared.Function;

public class RestService {
    private Executor executor;

    /*
    The Executor is responsible for executing the request and calling the success or error callbacks
     */
    @FunctionalInterface
    public interface Executor<T> {
         void execute(RestService.RestRequest<T> request);
    }

    public static class Failure extends RuntimeException {

        public int statusCode;

        public Failure() {
        }

        public Failure(String message) {
            super(message);
        }

        public Failure(int statusCode) {
            this.statusCode = statusCode;
        }

        public Failure(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }

    public static class RestRequest<T> {
        public Function<RestParams<T>,T> endpoint;
        public Consumer<T> successCallback;
        public Consumer<Exception> errorCallback;
        public String authorization;
        public String xsrfToken;
        private Executor executor;
        public int expectedStatusCode = 200;

        public RestRequest(Executor executor, Function<RestParams<T>,T> endpoint) {
            this.executor = executor;
            this.endpoint = endpoint;
        }

        public RestRequest<T> withBasicAuth(String authorization) {
            this.authorization = "Basic " + authorization;
            return this;
        }

        public RestRequest<T> withBearerAuth(String authorization) {
            this.authorization = "Bearer " + authorization;
            return this;
        }

        public RestRequest<T> withXsrfToken(String xsrfToken) {
            this.xsrfToken = xsrfToken;
            return this;
        }

        public RestRequest<T> withExecutor(Executor<T> executor) {
            this.executor = executor;
            return this;
        }

        public RestRequest<T> withExpectedStatusCode(int expectedStatusCode) {
            this.expectedStatusCode = expectedStatusCode;
            return this;
        }

        public RestRequest<T> onSuccess(Consumer<T> callback) {
            this.successCallback = callback;
            return this;
        }

        public RestRequest<T> onError(Consumer<Exception> callback) {
            this.errorCallback = callback;
            return this;
        }

        public void execute() {
            if (executor == null) {
                errorCallback.accept(new RestService.Failure(999, "No Executor to execute the request"));
                return;
            }

            executor.execute(this);
        }
    }

    public RestService() {}

    public RestService(Executor executor) {
        setExecutor(executor);
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public <T> RestRequest<T> request(Function<RestParams<T>,T> fn) {
        return new RestRequest<T>(executor, fn);
    }
}
