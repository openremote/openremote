package org.openremote.manager.shared.http;

import elemental.xml.XMLHttpRequest;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsType;

@JsType(isNative = true, namespace = "REST")
public class Request<T> {

    @FunctionalInterface
    @JsFunction
    public interface Callback<T> {
        void call(int responseCode, XMLHttpRequest xmlHttpRequest, T entity);
    }

    /**
     * Executes the request with all the information set in the current object. The value is never returned
     * but passed to the optional argument callback.
     */
    native public void execute(Callback<T> callback);

    /**
     * Sets the Accept request header. Defaults to star/star.
     */
    native public void setAccepts(String acceptHeader);

    /**
     * 	Sets the request credentials.
     */
    native public void setCredentials(String username, String password);

    /**
     * Sets the request entity.
     */
    native public void setEntity(String entity);

    /**
     * Sets the Content-Type request header.
     */
    native public void setContentType(String contentTypeHeader);

    /**
     * Sets the request URI. This should be an absolute URI.
     */
    native public void setURI(String uri);

    /**
     * Sets the request method. Defaults to GET.
     */
    native public void     setMethod(String method);

    /**
     * Controls whether the request should be asynchronous. Defaults to true.
     */
    native public void setAsync(boolean async);

    /**
     * Sets the given cookie in the current document when executing the request. Beware that this will
     * be persistent in your browser.
     */
    native public void addCookie(String name, String value);

    /**
     * Adds a query parameter to the URI query part.
     */
    native public void addQueryParameter(String name, String value);

    /**
     * Adds a matrix parameter (path parameter) to the last path segment of the request URI.
     */
    native public void addMatrixParameter(String name, String value);

    /**
     * Adds a request header.
     */
    native public void addHeader(String name, String value);
}
