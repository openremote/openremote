package org.openremote.manager.shared.model;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Created by Richard on 18/02/2016.
 */
public class Credentials {
    private String password = null;

    private String username = null;

    @NotNull
    @Size(min = 4, max = 12)
    public String getPassword() {
        return password;
    }

    @NotNull
    public String getUsername() {
        return username;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public void setUsername(final String username) {
        this.username = username;
    }
}
