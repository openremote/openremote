/*
 * Copyright 2016, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.manager.client.service;

import com.google.gwt.user.client.Cookies;

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
