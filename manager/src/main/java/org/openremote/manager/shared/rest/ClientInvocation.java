package org.openremote.manager.shared.rest;

import elemental.xml.XMLHttpRequest;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsType;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.core.HttpHeaders;

/**
 * TODO https://issues.jboss.org/browse/RESTEASY-1315
 */
@JsType
public class ClientInvocation<T> {

    @HeaderParam(HttpHeaders.AUTHORIZATION)
    @jsinterop.annotations.JsProperty(name = HttpHeaders.AUTHORIZATION)
    public String authorization;

    @jsinterop.annotations.JsProperty(name = "$callback")
    public Callback<T> callback;

    @JsIgnore
    public ClientInvocation() {
    }

    @JsIgnore
    public ClientInvocation(String accessToken) {
        setAccessToken(accessToken);
    }

    @JsIgnore
    public ClientInvocation<T> setAccessToken(String accessToken) {
        if (accessToken != null) {
            this.authorization = "Bearer " + accessToken;
        }
        return this;
    }

    @JsIgnore
    public ClientInvocation<T> with(Callback<T> callback) {
        this.callback = callback;
        return this;
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


    @JsFunction
    public interface Callback<T> {
        void call(int statusCode, XMLHttpRequest request, T entity);
    }

    public interface OnSuccess<T> {
        void call(T entity);
    }

    public interface OnFailure {
        void call(Failure failure);
    }

    public ClientInvocation<T> onResponse(int expectedStatusCode, OnSuccess<T> onSuccess, OnFailure onFailure) {
        if (callback != null)
            throw new IllegalStateException("Callback already set");

        callback = (statusCode, request, entity) -> {
            if (statusCode == 0) {
                onFailure.call(new Failure(0, "No response"));
            } else if (statusCode != expectedStatusCode) {
                onFailure.call(new Failure(statusCode, "Expected status code: " + expectedStatusCode));
            } else {
                onSuccess.call(entity);
            }
        };

        return this;
    }
}
