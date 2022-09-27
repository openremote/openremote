/*
 * Copyright 2020, OpenRemote Inc.
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
package org.openremote.manager.gateway;

import org.openremote.container.timer.TimerService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.gateway.GatewayClientResource;
import org.openremote.model.gateway.GatewayConnection;
import org.openremote.model.http.RequestParams;

import javax.ws.rs.WebApplicationException;
import java.util.Collections;
import java.util.List;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;

public class GatewayClientResourceImpl extends ManagerWebResource implements GatewayClientResource {

    protected GatewayClientService gatewayClientService;

    public GatewayClientResourceImpl(TimerService timerService,
                                     ManagerIdentityService identityService,
                                     GatewayClientService gatewayClientService) {
        super(timerService, identityService);
        this.gatewayClientService = gatewayClientService;
    }

    @Override
    public GatewayConnection getConnection(RequestParams requestParams, String realm, String id) {
        if (!realm.equals(getAuthenticatedRealmName()) && !isSuperUser()) {
            throw new WebApplicationException(FORBIDDEN);
        }

        try {
            return gatewayClientService.getConnections().stream().filter(
                c -> realm.equals(c.getLocalRealm()) && id.equals(c.getId())
            ).findFirst().orElse(null);
        } catch (Exception e) {
            throw new WebApplicationException(e, BAD_REQUEST);
        }
    }

    @Override
    public List<GatewayConnection> getRealmConnections(RequestParams requestParams, String realm) {
        if (!realm.equals(getAuthenticatedRealmName()) && !isSuperUser()) {
            throw new WebApplicationException(FORBIDDEN);
        }

        try {
            return gatewayClientService.getConnections().stream().filter(
                c -> realm.equals(c.getLocalRealm())
            ).toList();
        } catch (Exception e) {
            throw new WebApplicationException(e, BAD_REQUEST);
        }
    }

    @Override
    public ConnectionStatus getConnectionStatus(RequestParams requestParams, String realm, String id) {
        if (!realm.equals(getAuthenticatedRealmName()) && !isSuperUser()) {
            throw new WebApplicationException(FORBIDDEN);
        }

        return gatewayClientService.getConnectionStatus(realm, id);
    }

    @Override
    public List<GatewayConnection> getConnections(RequestParams requestParams) {
        if (!isSuperUser()) {
            throw new WebApplicationException(FORBIDDEN);
        }

        try {
            return gatewayClientService.getConnections();
        } catch (Exception e) {
            throw new WebApplicationException(e, BAD_REQUEST);
        }
    }

    @Override
    public void setConnection(RequestParams requestParams, String realm, GatewayConnection connection) {
        if (connection.getId() == null || connection.getId().isEmpty())
            connection.setId(realm);
        if (connection.getLocalUser() == null || connection.getLocalUser().isEmpty())
            connection.setLocalUser("");

        connection.setLocalRealm(realm);

        if (!connection.getLocalRealm().equals(getAuthenticatedRealmName()) && !isSuperUser()) {
            throw new WebApplicationException(FORBIDDEN);
        }

        try {
            gatewayClientService.setConnection(connection);
        } catch (Exception e) {
            throw new WebApplicationException(e, BAD_REQUEST);
        }
    }

    @Override
    public void deleteConnection(RequestParams requestParams, String realm, String id) {
        if (realm.isEmpty()) {
            throw new WebApplicationException(BAD_REQUEST);
        }

        if ((!getAuthenticatedRealmName().equals(realm)) && !isSuperUser()) {
            throw new WebApplicationException(FORBIDDEN);
        }

        try {
            boolean deleted = gatewayClientService.deleteConnection(realm, id);
            if (!deleted) {
                throw new WebApplicationException(BAD_REQUEST);
            }
        } catch (Exception e) {
            throw new WebApplicationException(e, BAD_REQUEST);
        }
    }

    @Override
    public void deleteConnections(RequestParams requestParams, List<String> realms) {
        if (realms.isEmpty()) {
            throw new WebApplicationException(BAD_REQUEST);
        }

        if ((realms.size() > 1 || !getAuthenticatedRealmName().equals(realms.get(0))) && !isSuperUser()) {
            throw new WebApplicationException(FORBIDDEN);
        }

        try {
            boolean deleted = gatewayClientService.deleteConnections(realms);
            if (!deleted) {
                throw new WebApplicationException(BAD_REQUEST);
            }
        } catch (Exception e) {
            throw new WebApplicationException(e, BAD_REQUEST);
        }
    }
}
