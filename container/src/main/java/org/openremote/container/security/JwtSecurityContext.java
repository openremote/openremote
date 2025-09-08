package org.openremote.container.security;

import jakarta.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.Set;

public class JwtSecurityContext implements SecurityContext {
    private final Principal principal;
    private final Set<String> roles;
    private final boolean secure;

    public JwtSecurityContext(String name, Set<String> roles, boolean secure) {
        this.principal = () -> name;
        this.roles = roles;
        this.secure = secure;
    }

    @Override public Principal getUserPrincipal() { return principal; }
    @Override public boolean isUserInRole(String role) { return roles != null && roles.contains(role); }
    @Override public boolean isSecure() { return secure; }
    @Override public String getAuthenticationScheme() { return "Bearer"; }
}
