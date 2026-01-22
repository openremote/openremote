package org.openremote.manager.gateway;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.gateway.GatewayServiceResource;
import org.openremote.model.gateway.GatewayTunnelInfo;
import org.openremote.model.http.RequestParams;
import org.openremote.model.util.TextUtil;

public class GatewayServiceResourceImpl extends ManagerWebResource implements GatewayServiceResource {

    protected GatewayService gatewayService;
    protected AssetStorageService assetStorageService;

    public GatewayServiceResourceImpl(TimerService timerService, ManagerIdentityService identityService, GatewayService gatewayService, AssetStorageService assetStorageService) {
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
    public GatewayTunnelInfo[] getGatewayActiveTunnelInfos(RequestParams requestParams, String realm, String gatewayId) {
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
                .filter(tunnel -> tunnel.getRealm().equals(realm) && tunnel.getGatewayId().equals(gatewayId))
                .toArray(GatewayTunnelInfo[]::new);
    }

    @Override
    public GatewayTunnelInfo getActiveTunnelInfo(RequestParams requestParams, String realm, String gatewayId, String target, int targetPort) {
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
            .filter(tunnel -> tunnel.getRealm().equals(realm) && tunnel.getGatewayId().equals(gatewayId) && tunnel.getTarget().equals(target) && tunnel.getTargetPort() == targetPort)
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

        if (isRestrictedUser() && !assetStorageService.isUserAsset(getUserId(), tunnelInfo.getGatewayId())) {
            throw new ForbiddenException();
        }

        try {
            return this.gatewayService.startTunnel(tunnelInfo);
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(e.getMessage(), Response.Status.BAD_REQUEST);
        } catch (IllegalStateException e) {
            throw new WebApplicationException(e.getMessage(), Response.Status.NOT_FOUND);
        } catch (RuntimeException e) {
            throw new WebApplicationException(e.getMessage(), Response.Status.EXPECTATION_FAILED);
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

        if (isRestrictedUser() && !assetStorageService.isUserAsset(getUserId(), tunnelInfo.getGatewayId())) {
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
