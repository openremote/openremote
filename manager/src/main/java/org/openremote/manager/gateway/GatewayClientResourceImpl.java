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

import jakarta.ws.rs.WebApplicationException;
import org.openremote.model.query.filter.RealmPredicate;

import java.util.Collections;
import java.util.List;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;

public class GatewayClientResourceImpl extends ManagerWebResource implements GatewayClientResource {

    protected GatewayClientService gatewayClientService;

    public GatewayClientResourceImpl(TimerService timerService,
                                     ManagerIdentityService identityService,
                                     GatewayClientService gatewayClientService) {
        super(timerService, identityService);
        this.gatewayClientService = gatewayClientService;
    }

    @Override
    public GatewayConnection getConnection(RequestParams requestParams, String realm) {
        if (!realm.equals(getAuthenticatedRealmName()) && !isSuperUser()) {
            throw new WebApplicationException(FORBIDDEN);
        }

        try {
            return gatewayClientService.getConnections().stream().filter(c -> realm.equals(c.getLocalRealm())).findFirst().orElse(null);
        } catch (Exception e) {
            throw new WebApplicationException(e, BAD_REQUEST);
        }
    }

    @Override
    public ConnectionStatus getConnectionStatus(RequestParams requestParams, String realm) {
        if (!realm.equals(getAuthenticatedRealmName()) && !isSuperUser()) {
            throw new WebApplicationException(FORBIDDEN);
        }

        return gatewayClientService.getConnectionStatus(realm);
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
        if (!realm.equals(getAuthenticatedRealmName()) && !isSuperUser()) {
            throw new WebApplicationException("Gateway connection can only be created in the users realm", FORBIDDEN);
        }

        connection.setLocalRealm(realm);

        // Force realm of any attribute filters
        if (connection.getAttributeFilters() != null) {
            connection.getAttributeFilters().forEach(filter -> {
                if (filter.getMatcher() != null) {
                    filter.getMatcher().realm(new RealmPredicate(realm));
                }
            });
        }

        try {
            gatewayClientService.setConnection(connection);
        } catch (Exception e) {
            throw new WebApplicationException(e, BAD_REQUEST);
        }
    }

    @Override
    public void deleteConnection(RequestParams requestParams, String realm) {
        deleteConnections(requestParams, Collections.singletonList(realm));
    }

    @Override
    public void deleteConnections(RequestParams requestParams, List<String> realms) {
        if (realms.isEmpty()) {
            throw new WebApplicationException(BAD_REQUEST);
        }

        if ((realms.size() > 1 || !getAuthenticatedRealmName().equals(realms.getFirst())) && !isSuperUser()) {
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
