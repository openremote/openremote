package org.openremote.manager.client.service;

import com.google.gwt.user.client.Cookies;

/**
 * Created by Richard on 10/02/2016.
 */
public class CookieServiceImpl implements CookieService {
    @Override
    public String getCookie(String name) {
        return Cookies.getCookie(name);
    }

    @Override
    public void setCookie(String name, String value) {
        Cookies.setCookie(name, value);
    }

    @Override
    public void removeCookie(String name) {
        Cookies.removeCookie(name);
    }
}
