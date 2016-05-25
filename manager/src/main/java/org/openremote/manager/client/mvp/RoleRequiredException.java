package org.openremote.manager.client.mvp;

public class RoleRequiredException extends Exception {

    final String requiredRole;

    public RoleRequiredException() {
        this(null);
    }

    public RoleRequiredException(String requiredRole) {
        this.requiredRole = requiredRole;
    }

    public String getRequiredRole() {
        return requiredRole;
    }
}
