package org.openremote.container.security;

import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;

import java.security.Principal;
import java.util.Set;

public final class JwtAccount implements Account {
    private final Principal principal;
    private final Set<String> roles;

    public JwtAccount(String name, Set<String> roles) {
        this.principal = () -> name;
        this.roles = roles;
    }
    @Override public Principal getPrincipal() { return principal; }
    @Override public Set<String> getRoles() { return roles; }
}

public final class PassthroughIdentityManager implements IdentityManager {
    @Override public Account verify(Account account) { return account; }
    @Override public Account verify(String id, Credential credential) { return null; }
    @Override public Account verify(Credential credential) { return null; }
}

