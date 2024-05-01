package org.openremote.manager.gateway;

import org.openremote.container.timer.TimerService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.gateway.GatewayServiceResource;
import org.openremote.model.gateway.TunnelInfo;
import org.openremote.model.http.RequestParams;

public class GatewayServiceResourceImpl extends ManagerWebResource implements GatewayServiceResource {

    protected GatewayService gatewayService;

    public GatewayServiceResourceImpl(TimerService timerService, ManagerIdentityService identityService, GatewayService gatewayService) {
        super(timerService, identityService);
        this.gatewayService = gatewayService;
    }

    @Override
    public TunnelInfo[] getActiveTunnelInfos(RequestParams requestParams, String realm) {
        return new TunnelInfo[0];
    }

    @Override
    public TunnelInfo getActiveTunnelInfoById(RequestParams requestParams, String realm, String tunnelId) {
        return null;
    }

    @Override
    public void startTunnel(TunnelInfo tunnelInfo) {
        this.gatewayService.tryStartTunnel(tunnelInfo);
    }

    @Override
    public void stopTunnel(TunnelInfo tunnelInfo) {
        this.gatewayService.tryStopTunnel(tunnelInfo);
    }
}
