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
package org.openremote.agent.protocol.http;

import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TextUtil;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

/**
 * A filter to support OAuth access token (including refresh)
 */
public class OAuthFilter implements ClientRequestFilter {

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, OAuthFilter.class);
    public static final String AUTH_TYPE = "Bearer";
    protected OAuthServerResponse authServerResponse;
    protected WebTarget authTarget;
    protected OAuthGrant oAuthGrant;

    public OAuthFilter(WebTarget authTarget, OAuthGrant oAuthGrant) {
        Objects.requireNonNull(authTarget);
        Objects.requireNonNull(oAuthGrant);
        this.authTarget = authTarget;
        this.oAuthGrant = oAuthGrant;
    }

    public String getAuthHeader() {
        String accessToken = getAccessToken();
        if (!TextUtil.isNullOrEmpty(accessToken)) {
            return AUTH_TYPE + " " + getAccessToken();
        }

        return null;
    }

    public synchronized String getAccessToken() {
        LocalDateTime expiryDateTime = authServerResponse == null ? null : authServerResponse.getExpiryDateTime();
        boolean updateRequired = expiryDateTime == null || expiryDateTime.minusSeconds(10).isBefore(LocalDateTime.now());

        if (updateRequired) {
            updateToken();
        }

        return authServerResponse != null ? authServerResponse.accessToken : null;
    }

    protected synchronized void updateToken() {
        LOG.info("Updating OAuth token");
        Response response;

        if (authServerResponse != null && authServerResponse.refreshToken != null) {
            // Do a refresh
            LOG.fine("Using Refresh grant");
            response = requestTokenUsingRefresh();
            if (response.getStatus() == 403) {
                // Maybe the refresh token is not valid so do full auth
                LOG.warning("OAuth token refresh failed, trying a full authentication");
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
        } else {
            authServerResponse = response.readEntity(OAuthServerResponse.class);
            LOG.finest("OAuth server successfully returned an access token");
        }
    }

    protected Response requestTokenUsingRefresh() {
        OAuthRefreshTokenGrant refreshGrant = new OAuthRefreshTokenGrant(
            oAuthGrant.getTokenEndpointUri(),
            oAuthGrant.getClientId(),
            oAuthGrant.getClientSecret(),
            oAuthGrant.getScope(),
            authServerResponse.refreshToken
        );

        return authTarget
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.entity(new Form(refreshGrant.valueMap), MediaType.APPLICATION_FORM_URLENCODED_TYPE));
    }

    protected Response requestToken() {
        return authTarget
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.entity(new Form(oAuthGrant.valueMap), MediaType.APPLICATION_FORM_URLENCODED_TYPE));
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        requestContext.getHeaders().putSingle(HttpHeaders.AUTHORIZATION, getAuthHeader());
    }
}
