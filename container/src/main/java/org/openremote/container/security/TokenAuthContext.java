package org.openremote.container.security;

/**
 * An {@link AuthContext} implementation that wraps a {@link TokenPrincipal}.
 */
public class TokenAuthContext implements AuthContext {

    protected TokenPrincipal principal;

    @Override
    public String getAuthenticatedRealmName() {
        return principal.getRealm();
    }

    @Override
    public String getUsername() {
        return principal.getUsername();
    }

    @Override
    public String getUserId() {
        return principal.getSubject();
    }

    @Override
    public String getClientId() {
        return principal.getClientId();
    }

    @Override
    public boolean hasRealmRole(String role) {
        return false;
    }

    @Override
    public boolean hasResourceRole(String role, String resource) {
        return false;
    }
}
