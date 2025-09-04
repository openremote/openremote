package org.openremote.container.security;

import jakarta.security.enterprise.credential.Credential;

public final class JwtCredential implements Credential {
    private final String token;
    public JwtCredential(String token) { this.token = token; }
    public String token() { return token; }
}
