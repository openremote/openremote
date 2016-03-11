package org.openremote.manager.shared.rpc;

public class Failure extends RuntimeException {

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
