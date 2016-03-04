package org.openremote.manager.server2.identity;

import javax.ws.rs.FormParam;

public class AuthForm {

    @FormParam("client_id")
    public String clientId;

    @FormParam("username")
    public String username;

    @FormParam("password")
    public String password;

    @FormParam("grant_type")
    public String grantType;

    public AuthForm() {
    }

    public AuthForm(String clientId, String username, String password) {
        this(clientId, username, password, "password");
    }

    public AuthForm(String clientId, String username, String password, String grantType) {
        this.clientId = clientId;
        this.username = username;
        this.password = password;
        this.grantType = grantType;
    }
}

