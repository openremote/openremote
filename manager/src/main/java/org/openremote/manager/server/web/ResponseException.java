package org.openremote.manager.server.web;

public class ResponseException extends RuntimeException {

    final protected int statusCode;

    public ResponseException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
