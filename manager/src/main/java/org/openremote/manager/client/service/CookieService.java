package org.openremote.manager.client.service;

/**
 * Created by Richard on 10/02/2016.
 */
public interface CookieService {
    String getCookie(String name);

    void setCookie(String name, String value);

    void removeCookie(String name);
}
