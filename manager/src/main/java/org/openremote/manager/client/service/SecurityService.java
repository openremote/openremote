package org.openremote.manager.client.service;

import java.util.Date;

/**
 * Created by Richard on 10/02/2016.
 */
public interface SecurityService {
    String getUsername();

    boolean isTokenExpired();

    boolean hasToken();

    Date getTokenExpirationDate();

    String getXsrfToken();

    void logout();
}
