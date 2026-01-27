/*
 * Copyright 2017, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.container.web;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

import java.io.IOException;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.logging.Logger;

import org.jboss.resteasy.util.BasicAuthHelper;
import org.openremote.model.auth.OAuthGrant;
import org.openremote.model.auth.OAuthRefreshTokenGrant;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TextUtil;

import jakarta.ws.rs.client.*;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** A filter to support OAuth access token (including refresh) */
public class OAuthFilter implements ClientRequestFilter {

  private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, OAuthFilter.class);
  public static final String BEARER_AUTH = "Bearer";
  protected OAuthServerResponse authServerResponse;
  protected Client client;
  protected WebTarget authTarget;
  protected OAuthGrant oAuthGrant;

  public OAuthFilter(Client client, OAuthGrant oAuthGrant) {
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
    LocalDateTime expiryDateTime =
        authServerResponse == null ? null : authServerResponse.getExpiryDateTime();
    boolean updateRequired =
        expiryDateTime == null || expiryDateTime.minusSeconds(10).isBefore(LocalDateTime.now());

    if (updateRequired) {
      updateToken();
    }

    return authServerResponse != null ? authServerResponse.accessToken : null;
  }

  protected synchronized void updateToken() throws SocketException {
    LOG.finest("Updating OAuth token: " + oAuthGrant.getTokenEndpointUri());
    Response response = null;

    try {
      if (authServerResponse != null && authServerResponse.refreshToken != null) {
        // Do a refresh
        LOG.finest("Using Refresh grant");
        response = requestTokenUsingRefresh();
        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
          // Maybe the refresh token is not valid so do full auth
          LOG.finest("OAuth token refresh failed, trying a full authentication");
          authServerResponse = null;
          updateToken();
          return;
        }
      } else {
        // Do full auth
        LOG.finest("Doing full authentication");
        response = requestToken();
      }

      if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
        authServerResponse = null;
        LOG.fine(
            "OAuth server response error '"
                + response.getStatus()
                + "': "
                + oAuthGrant.getTokenEndpointUri());
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
    OAuthRefreshTokenGrant refreshGrant =
        new OAuthRefreshTokenGrant(
            oAuthGrant.getTokenEndpointUri(),
            oAuthGrant.getClientId(),
            oAuthGrant.getClientSecret(),
            oAuthGrant.getScope(),
            authServerResponse.refreshToken);

    return authTarget
        .request(MediaType.APPLICATION_JSON_TYPE)
        .post(
            Entity.entity(
                new Form(refreshGrant.asMultivaluedMap()),
                MediaType.APPLICATION_FORM_URLENCODED_TYPE));
  }

  protected Response requestToken() throws SocketException {
    Invocation.Builder builder = authTarget.request(MediaType.APPLICATION_JSON_TYPE);

    if (oAuthGrant.isBasicAuthHeader()) {
      builder.header(
          HttpHeaders.AUTHORIZATION,
          BasicAuthHelper.createHeader(oAuthGrant.getClientId(), oAuthGrant.getClientSecret()));
    }
    return builder.post(
        Entity.entity(
            new Form(oAuthGrant.asMultivaluedMap()), MediaType.APPLICATION_FORM_URLENCODED_TYPE));
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
