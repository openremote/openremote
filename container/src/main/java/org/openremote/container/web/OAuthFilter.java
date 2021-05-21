/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.container.web;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.util.BasicAuthHelper;
import org.openremote.model.auth.OAuthGrant;
import org.openremote.model.auth.OAuthRefreshTokenGrant;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TextUtil;

import javax.ws.rs.client.*;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

/**
 * A filter to support OAuth access token (including refresh)
 */
public class OAuthFilter implements ClientRequestFilter {

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, OAuthFilter.class);
    public static final String BEARER_AUTH = "Bearer";
    protected OAuthServerResponse authServerResponse;
    protected ResteasyClient client;
    protected WebTarget authTarget;
    protected OAuthGrant oAuthGrant;

    public OAuthFilter(ResteasyClient client, OAuthGrant oAuthGrant) {
        Objects.requireNonNull(client);
        Objects.requireNonNull(oAuthGrant);
        this.client = client;
        this.authTarget = client.target(oAuthGrant.getTokenEndpointUri());
        this.oAuthGrant = oAuthGrant;
    }

    public String getAuthHeader() throws SocketException {
        String accessToken = getAccessToken();

        if (!TextUtil.isNullOrEmpty(accessToken)) {
            return BEARER_AUTH + " " + accessToken;
        }

        return null;
    }

    public synchronized String getAccessToken() throws SocketException {
        LocalDateTime expiryDateTime = authServerResponse == null ? null : authServerResponse.getExpiryDateTime();
        boolean updateRequired = expiryDateTime == null || expiryDateTime.minusSeconds(10).isBefore(LocalDateTime.now());

        if (updateRequired) {
            updateToken();
        }

        return authServerResponse != null ? authServerResponse.accessToken : null;
    }

    protected synchronized void updateToken() throws SocketException {
        LOG.fine("Updating OAuth token");
        Response response = null;

        try {
            if (authServerResponse != null && authServerResponse.refreshToken != null) {
                // Do a refresh
                LOG.fine("Using Refresh grant");
                response = requestTokenUsingRefresh();
                if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                    // Maybe the refresh token is not valid so do full auth
                    LOG.info("OAuth token refresh failed, trying a full authentication");
                    authServerResponse = null;
                    updateToken();
                    return;
                }
            } else {
                // Do full auth
                LOG.fine("Doing full authentication");
                response = requestToken();
            }

            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                authServerResponse = null;
                LOG.warning("OAuth server response error: " + response.getStatus());
                throw new RuntimeException("OAuth server response error: " + response.getStatus());
            } else {
                authServerResponse = response.readEntity(OAuthServerResponse.class);
                LOG.finest("OAuth server successfully returned an access token");
            }
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    protected Response requestTokenUsingRefresh() throws SocketException {
        OAuthRefreshTokenGrant refreshGrant = new OAuthRefreshTokenGrant(
            oAuthGrant.getTokenEndpointUri(),
            oAuthGrant.getClientId(),
            oAuthGrant.getClientSecret(),
            oAuthGrant.getScope(),
            authServerResponse.refreshToken
        );

        return authTarget
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.entity(new Form(refreshGrant.asMultivaluedMap()), MediaType.APPLICATION_FORM_URLENCODED_TYPE));
    }

    protected Response requestToken() throws SocketException {
        Invocation.Builder builder = authTarget
            .request(MediaType.APPLICATION_JSON_TYPE);

        if (oAuthGrant.isBasicAuthHeader()) {
            builder.header(HttpHeaders.AUTHORIZATION, BasicAuthHelper.createHeader(oAuthGrant.getClientId(), oAuthGrant.getClientSecret()));
        }
        return builder.post(Entity.entity(new Form(oAuthGrant.asMultivaluedMap()), MediaType.APPLICATION_FORM_URLENCODED_TYPE));
    }

    public synchronized void updateGrant(OAuthGrant grant) {
        this.authServerResponse = null;
        this.oAuthGrant = grant;
        this.authTarget = client.target(oAuthGrant.getTokenEndpointUri());
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        requestContext.getHeaders().putSingle(HttpHeaders.AUTHORIZATION, getAuthHeader());
    }
}
