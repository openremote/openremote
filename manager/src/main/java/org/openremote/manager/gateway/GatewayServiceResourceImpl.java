package org.openremote.manager.gateway;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.gateway.GatewayServiceResource;
import org.openremote.model.gateway.GatewayTunnelInfo;
import org.openremote.model.http.RequestParams;
import org.openremote.model.util.TextUtil;

public class GatewayServiceResourceImpl extends ManagerWebResource implements GatewayServiceResource {

    protected GatewayService gatewayService;

    public GatewayServiceResourceImpl(TimerService timerService, ManagerIdentityService identityService, GatewayService gatewayService) {
        super(timerService, identityService);
        this.gatewayService = gatewayService;
    }

    @Override
    public GatewayTunnelInfo[] getActiveTunnelInfos(RequestParams requestParams, String realm) {
        if (TextUtil.isNullOrEmpty(realm)) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        if (!isAuthenticated() || !realm.equals(getAuthenticatedRealmName())) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        return this.gatewayService.getTunnelInfos().stream()
                .filter(tunnel -> tunnel.getRealm().equals(realm))
                .toArray(GatewayTunnelInfo[]::new);
    }

    @Override
    public GatewayTunnelInfo getActiveTunnelInfoByGatewayId(RequestParams requestParams, String realm, String gatewayId) {
        if (TextUtil.isNullOrEmpty(realm) || !isAuthenticated() || !realm.equals(getAuthenticatedRealmName())) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        if (TextUtil.isNullOrEmpty(gatewayId)) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        GatewayTunnelInfo[] infos = this.gatewayService.getTunnelInfos().stream()
                .filter(tunnel -> tunnel.getRealm().equals(realm) && tunnel.getGatewayId().equals(gatewayId))
                .limit(1)
                .toArray(GatewayTunnelInfo[]::new);

        if(infos.length == 0) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        return infos[0];
    }

    @Override
    public GatewayTunnelInfo startTunnel(GatewayTunnelInfo tunnelInfo) {
        if(tunnelInfo == null) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        try {
            return this.gatewayService.tryStartTunnel(tunnelInfo);
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(e.getMessage(), Response.Status.BAD_REQUEST);
        } catch (IllegalStateException e) {
            throw new WebApplicationException(e.getMessage(), Response.Status.NOT_FOUND);
        }
    }

    @Override
    public GatewayTunnelInfo stopTunnel(GatewayTunnelInfo tunnelInfo) {
        if(tunnelInfo == null) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        try {
            return this.gatewayService.tryStopTunnel(tunnelInfo);
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(e.getMessage(), Response.Status.BAD_REQUEST);
        } catch (IllegalStateException e) {
            throw new WebApplicationException(e.getMessage(), Response.Status.NOT_FOUND);
        }
    }
}
