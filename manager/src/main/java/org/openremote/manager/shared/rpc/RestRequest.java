package org.openremote.manager.shared.rpc;

import org.openremote.manager.shared.Consumer;
import org.openremote.manager.shared.Function;

public class RestRequest<T, U extends RequestData> {
    public Function<U,T> endpoint;
    public Consumer<T> successCallback;
    public U params;
    public Consumer<Exception> errorCallback;
    public String authorization;
    public String xsrfToken;
    public String sendData;
    protected Executor executor;

    public int expectedStatusCode = 200;

    public RestRequest(Executor executor, Function<U,T> endpoint, U params) {
        this.executor = executor;
        this.endpoint = endpoint;
        this.params = params;
    }

    public RestRequest<T, U> withBasicAuth(String authorization) {
        this.authorization = "Basic " + authorization;
        return this;
    }

    public RestRequest<T, U> withBearerAuth(String authorization) {
        this.authorization = "Bearer " + authorization;
        return this;
    }

    public RestRequest<T, U> withXsrfToken(String xsrfToken) {
        this.xsrfToken = xsrfToken;
        return this;
    }

    public RestRequest<T, U> withExecutor(Executor<T> executor) {
        this.executor = executor;
        return this;
    }

    public RestRequest<T, U> withExpectedStatusCode(int expectedStatusCode) {
        this.expectedStatusCode = expectedStatusCode;
        return this;
    }

    public RestRequest<T, U> withSendData(String data) {
        this.sendData = data;
        return this;
    }

    public RestRequest<T, U> onSuccess(Consumer<T> callback) {
        this.successCallback = callback;
        return this;
    }

    public RestRequest<T, U> onError(Consumer<Exception> callback) {
        this.errorCallback = callback;
        return this;
    }

    public void execute() {
        if (executor == null) {
            errorCallback.accept(new Failure("No Executor to execute the request"));
            return;
        }

        try {
            executor.execute(this);
        } catch (Exception e) {
            if (errorCallback != null) {
                errorCallback.accept(new Failure(e.getMessage()));
            }
        }
    }
}
