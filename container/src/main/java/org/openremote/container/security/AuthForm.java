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
package org.openremote.container.security;

import javax.ws.rs.FormParam;

public class AuthForm {

    @FormParam("client_id")
    public String clientId;

    @FormParam("username")
    public String username;

    @FormParam("password")
    public String password;

    @FormParam("grant_type")
    public String grantType;

    public AuthForm() {
    }

    public AuthForm(String clientId, String username, String password) {
        this(clientId, username, password, "password");
    }

    public AuthForm(String clientId, String username, String password, String grantType) {
        this.clientId = clientId;
        this.username = username;
        this.password = password;
        this.grantType = grantType;
    }
}

