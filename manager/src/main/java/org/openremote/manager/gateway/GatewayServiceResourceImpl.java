package org.openremote.manager.gateway;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.gateway.GatewayServiceResource;
import org.openremote.model.gateway.GatewayTunnelInfo;
import org.openremote.model.http.RequestParams;

public class GatewayServiceResourceImpl extends ManagerWebResource implements GatewayServiceResource {

    protected GatewayService gatewayService;

    public GatewayServiceResourceImpl(TimerService timerService, ManagerIdentityService identityService, GatewayService gatewayService) {
        super(timerService, identityService);
        this.gatewayService = gatewayService;
    }

    @Override
    public GatewayTunnelInfo[] getActiveTunnelInfos(RequestParams requestParams, String realm) {
        return new GatewayTunnelInfo[0];
    }

    @Override
    public GatewayTunnelInfo getActiveTunnelInfoById(RequestParams requestParams, String realm, String tunnelId) {
        return null;
    }

    @Override
    public void startTunnel(GatewayTunnelInfo tunnelInfo) {
        try {
            this.gatewayService.tryStartTunnel(tunnelInfo);
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(e.getMessage(), Response.Status.BAD_REQUEST);
        } catch (IllegalStateException e) {
            throw new WebApplicationException(e.getMessage(), Response.Status.NOT_FOUND);
        }
    }

    @Override
    public void stopTunnel(GatewayTunnelInfo tunnelInfo) {
        try {
            this.gatewayService.tryStopTunnel(tunnelInfo);
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(e.getMessage(), Response.Status.BAD_REQUEST);
        } catch (IllegalStateException e) {
            throw new WebApplicationException(e.getMessage(), Response.Status.NOT_FOUND);
        }
    }
}
