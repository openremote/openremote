package org.openremote.container.security;

import jakarta.security.enterprise.AuthenticationException;

@FunctionalInterface
public interface TokenVerifier {
    TokenPrincipal verify(String realm, String accessToken) throws AuthenticationException;
}
