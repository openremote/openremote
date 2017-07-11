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

import com.google.gwt.user.client.Window;
import elemental.client.Browser;
import org.openremote.manager.shared.http.RequestParams;
import org.openremote.model.Constants;

import java.util.logging.Logger;

/**
 * Basic authorization, any user has all roles and is superuser.
 */
public class BasicSecurityService implements SecurityService {

    private static final Logger LOG = Logger.getLogger(BasicSecurityService.class.getName());

    protected final String username;
    protected final String password;

    public BasicSecurityService(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getFullName() {
        return username;
    }

    @Override
    public void logout() {
        LOG.fine("Logging out...");
        String url = Window.Location.getProtocol() + "//" + Window.Location.getHost() + "/" + Constants.MASTER_REALM;
        Window.Location.assign(url);
    }

    @Override
    public boolean isSuperUser() {
        return true;
    }

    @Override
    public boolean hasRealmRole(String role) {
        return true;
    }

    @Override
    public boolean hasResourceRole(String role, String resource) {
        return true;
    }

    @Override
    public boolean hasResourceRoleOrIsSuperUser(String role, String resource) {
        return true;
    }

    @Override
    public String getAuthenticatedRealm() {
        return Constants.MASTER_REALM;
    }

    @Override
    public <OUT> void setCredentials(RequestParams<OUT> requestParams) {
        requestParams.setUsername(username);
        requestParams.setPassword(password);
    }

    @Override
    public String setCredentials(String serviceUrl) {
        String authenticatedServiceUrl = serviceUrl
            + "?Auth-Realm=" + getAuthenticatedRealm()
            + "&Authorization=Basic " + Browser.getWindow().btoa(username + ":" + password);
        return authenticatedServiceUrl;
    }

}
