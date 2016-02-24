package org.openremote.manager.shared.model;

/**
 * Created by Richard on 22/02/2016.
 */
public class LoginResult {
    private int httpResponse;
    private String token;

    public LoginResult() {
    }

    public LoginResult(int httpResponse, String token) {
        this.httpResponse = httpResponse;
        this.token = token;
    }

    public int getHttpResponse() {
        return httpResponse;
    }

    public void setHttpResponse(int httpResponse) {
        this.httpResponse = httpResponse;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
