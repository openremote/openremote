package org.openremote.manager.shared.http;

import elemental.xml.XMLHttpRequest;
import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import org.openremote.manager.shared.Consumer;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.core.HttpHeaders;

/**
 * TODO https://issues.jboss.org/browse/RESTEASY-1315
 */
@JsType
public class RequestParams<T> {

    public static final int ANY_STATUS_CODE = -1;

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

    @HeaderParam(HttpHeaders.AUTHORIZATION)
    @JsProperty(name = HttpHeaders.AUTHORIZATION)
    public String authorization;

    @HeaderParam("XSRF")
    @JsProperty(name= "XSRF")
    public String xsrfToken;

    @JsProperty
    public String entity;

    @JsProperty(name = "$contentType")
    public String contentType;

    @JsProperty(name = "$accepts")
    public String accepts;

    @JsProperty(name = "$apiURL")
    public String apiURL;

    @JsProperty(name = "$username")
    public String username;

    @JsProperty(name = "$password")
    public String password;

    public int expectedStatusCode = ANY_STATUS_CODE;

    @JsProperty(name = "$callback")
    public Request.Callback callback;

    @JsIgnore
    public RequestParams() {
    }

    @JsIgnore
    public RequestParams(int expectedStatusCode, Consumer<T> onSuccess, Consumer<Exception> onFailure) {
        this.expectedStatusCode = expectedStatusCode;
        this.callback = new Request.Callback<T>() {
            @Override
            public void call(int responseCode, XMLHttpRequest xmlHttpRequest, T entity) {
                if (responseCode == 0) {
                    onFailure.accept(new RequestParams.Failure(0, "No response"));
                    return;
                }
                if (expectedStatusCode != -1 && responseCode != expectedStatusCode) {
                    onFailure.accept(new RequestParams.Failure(responseCode, "Expected status code: " + expectedStatusCode));
                    return;
                }
                onSuccess.accept(entity);
            }
        };
    }

    public RequestParams<T> withBearerAuth(String authorization) {
        this.authorization = "Bearer " + authorization;
        return this;
    }

    public String getBearerAuth() {
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.split(" ").length != 2)
            return null;
        return authorization.split(" ")[1];
    }

    public RequestParams<T> setXsrfToken(String xsrfToken) {
        this.xsrfToken = xsrfToken;
        return this;
    }

    public RequestParams<T> setExpectedStatusCode(int expectedStatusCode) {
        this.expectedStatusCode = expectedStatusCode;
        return this;
    }

    public RequestParams<T> setEntity(String entity) {
        this.entity = entity;
        return this;
    }

    public RequestParams<T> setContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public RequestParams<T> setAccepts(String accepts) {
        this.accepts = accepts;
        return this;
    }

    public RequestParams<T> setApiURL(String apiURL) {
        this.apiURL = apiURL;
        return this;
    }

    public RequestParams<T> setUsername(String username) {
        this.username = username;
        return this;
    }

    public RequestParams<T> setPassword(String password) {
        this.password = password;
        return this;
    }

}
