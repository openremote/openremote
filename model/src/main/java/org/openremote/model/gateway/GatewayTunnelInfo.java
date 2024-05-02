package org.openremote.model.gateway;

public class GatewayTunnelInfo {

    protected String id;
    protected String gatewayId;
    protected String realm;
    protected int targetPort;
    protected String target;

    public GatewayTunnelInfo() {
        this.targetPort = 443;
        this.target = "localhost";
    }

    public GatewayTunnelInfo(String id, String gatewayId, String realm) {
        this.id = id;
        this.gatewayId = gatewayId;
        this.realm = realm;
        this.targetPort = 443;
        this.target = "localhost";
    }

    public GatewayTunnelInfo(String id, String gatewayId, String realm, int targetPort, String target) {
        this.id = id;
        this.gatewayId = gatewayId;
        this.realm = realm;
        this.targetPort = targetPort;
        this.target = target;
    }

    public String getId() {
        return id;
    }

    public String getGatewayId() {
        return gatewayId;
    }

    public String getRealm() {
        return realm;
    }

    public int getTargetPort() {
        return targetPort;
    }

    public String getTarget() {
        return target;
    }

    public GatewayTunnelInfo setId(String id) {
        this.id = id;
        return this;
    }

    public GatewayTunnelInfo setGatewayId(String gatewayId) {
        this.gatewayId = gatewayId;
        return this;
    }

    public GatewayTunnelInfo setRealm(String realm) {
        this.realm = realm;
        return this;
    }

    public GatewayTunnelInfo setTargetPort(int targetPort) {
        this.targetPort = targetPort;
        return this;
    }

    public GatewayTunnelInfo setTarget(String target) {
        this.target = target;
        return this;
    }
}
