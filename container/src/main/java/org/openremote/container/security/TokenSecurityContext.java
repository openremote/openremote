package org.openremote.container.security;

import java.security.Principal;
import java.util.Set;

import jakarta.ws.rs.core.SecurityContext;

public class TokenSecurityContext implements SecurityContext {

    private final Principal principal;
    private final Set<String> roles;
    private final boolean secure;
    private final String authenticationScheme;

    public TokenSecurityContext(Principal principal, Set<String> roles, boolean secure, String authenticationScheme) {
        this.principal = principal;
        this.roles = roles;
        this.secure = secure;
        this.authenticationScheme = authenticationScheme;
    }

    @Override
    public Principal getUserPrincipal() {
        return principal;
    }

    @Override
    public boolean isUserInRole(String role) {
        return roles.contains(role);
    }

    @Override
    public boolean isSecure() {
        return secure;
    }

    @Override
    public String getAuthenticationScheme() {
        return authenticationScheme;
    }
}
