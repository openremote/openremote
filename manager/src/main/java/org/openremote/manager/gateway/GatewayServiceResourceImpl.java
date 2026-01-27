/*
 * Copyright 2026, OpenRemote Inc.
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
package org.openremote.manager.gateway;

import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.gateway.GatewayServiceResource;
import org.openremote.model.gateway.GatewayTunnelInfo;
import org.openremote.model.http.RequestParams;
import org.openremote.model.util.TextUtil;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

public class GatewayServiceResourceImpl extends ManagerWebResource
    implements GatewayServiceResource {

  protected GatewayService gatewayService;
  protected AssetStorageService assetStorageService;

  public GatewayServiceResourceImpl(
      TimerService timerService,
      ManagerIdentityService identityService,
      GatewayService gatewayService,
      AssetStorageService assetStorageService) {
    super(timerService, identityService);
    this.gatewayService = gatewayService;
    this.assetStorageService = assetStorageService;
  }

  @Override
  public GatewayTunnelInfo[] getAllActiveTunnelInfos(RequestParams requestParams, String realm) {
    if (TextUtil.isNullOrEmpty(realm) || !isAuthenticated()) {
      throw new WebApplicationException(Response.Status.FORBIDDEN);
    }

    if (!isSuperUser() && !getAuthenticatedRealmName().equals(realm)) {
      throw new WebApplicationException(Response.Status.FORBIDDEN);
    }

    if (isRestrictedUser()) {
      throw new WebApplicationException(Response.Status.FORBIDDEN);
    }

    return this.gatewayService.getTunnelInfos().stream()
        .filter(tunnel -> tunnel.getRealm().equals(realm))
        .toArray(GatewayTunnelInfo[]::new);
  }

  @Override
  public GatewayTunnelInfo[] getGatewayActiveTunnelInfos(
      RequestParams requestParams, String realm, String gatewayId) {
    if (TextUtil.isNullOrEmpty(realm) || !isAuthenticated()) {
      throw new WebApplicationException(Response.Status.FORBIDDEN);
    }

    if (!isSuperUser() && !getAuthenticatedRealmName().equals(realm)) {
      throw new WebApplicationException(Response.Status.FORBIDDEN);
    }

    if (TextUtil.isNullOrEmpty(gatewayId)) {
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }

    if (isRestrictedUser() && !assetStorageService.isUserAsset(getUserId(), gatewayId)) {
      throw new WebApplicationException(Response.Status.FORBIDDEN);
    }

    return this.gatewayService.getTunnelInfos().stream()
        .filter(
            tunnel -> tunnel.getRealm().equals(realm) && tunnel.getGatewayId().equals(gatewayId))
        .toArray(GatewayTunnelInfo[]::new);
  }

  @Override
  public GatewayTunnelInfo getActiveTunnelInfo(
      RequestParams requestParams, String realm, String gatewayId, String target, int targetPort) {
    if (TextUtil.isNullOrEmpty(realm) || !isAuthenticated()) {
      throw new WebApplicationException(Response.Status.FORBIDDEN);
    }

    if (!isSuperUser() && !getAuthenticatedRealmName().equals(realm)) {
      throw new WebApplicationException(Response.Status.FORBIDDEN);
    }

    if (TextUtil.isNullOrEmpty(gatewayId)) {
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }

    if (isRestrictedUser() && !assetStorageService.isUserAsset(getUserId(), gatewayId)) {
      throw new WebApplicationException(Response.Status.FORBIDDEN);
    }

    return this.gatewayService.getTunnelInfos().stream()
        .filter(
            tunnel ->
                tunnel.getRealm().equals(realm)
                    && tunnel.getGatewayId().equals(gatewayId)
                    && tunnel.getTarget().equals(target)
                    && tunnel.getTargetPort() == targetPort)
        .findFirst()
        .orElse(null);
  }

  @Override
  public GatewayTunnelInfo startTunnel(GatewayTunnelInfo tunnelInfo) {
    if (!isAuthenticated()) {
      throw new ForbiddenException();
    }

    if (tunnelInfo == null || TextUtil.isNullOrEmpty(tunnelInfo.getGatewayId())) {
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }

    if (TextUtil.isNullOrEmpty(tunnelInfo.getRealm())) {
      tunnelInfo.setRealm(getAuthenticatedRealmName());
    } else if (!isSuperUser() && !tunnelInfo.getRealm().equals(getAuthenticatedRealmName())) {
      throw new ForbiddenException();
    }

    if (isRestrictedUser()
        && !assetStorageService.isUserAsset(getUserId(), tunnelInfo.getGatewayId())) {
      throw new ForbiddenException();
    }

    try {
      return this.gatewayService.startTunnel(tunnelInfo);
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), Response.Status.BAD_REQUEST);
    } catch (IllegalStateException e) {
      throw new WebApplicationException(e.getMessage(), Response.Status.NOT_FOUND);
    } catch (RuntimeException e) {
      throw new WebApplicationException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public void stopTunnel(GatewayTunnelInfo tunnelInfo) {
    if (!isAuthenticated()) {
      throw new ForbiddenException();
    }

    if (tunnelInfo == null || TextUtil.isNullOrEmpty(tunnelInfo.getGatewayId())) {
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }

    if (TextUtil.isNullOrEmpty(tunnelInfo.getRealm())) {
      tunnelInfo.setRealm(getAuthenticatedRealmName());
    } else if (!isSuperUser() && !tunnelInfo.getRealm().equals(getAuthenticatedRealmName())) {
      throw new ForbiddenException();
    }

    if (isRestrictedUser()
        && !assetStorageService.isUserAsset(getUserId(), tunnelInfo.getGatewayId())) {
      throw new ForbiddenException();
    }

    try {
      this.gatewayService.stopTunnel(tunnelInfo);
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), Response.Status.BAD_REQUEST);
    } catch (IllegalStateException e) {
      throw new WebApplicationException(e.getMessage(), Response.Status.NOT_FOUND);
    } catch (RuntimeException e) {
      throw new WebApplicationException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
    }
  }
}
