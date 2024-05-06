package org.openremote.model.gateway;

public class GatewayTunnelInfo {

    protected String id;
    protected String gatewayId;
    protected String realm;
    protected int targetPort;
    protected String target;
    protected String protocol;

    public GatewayTunnelInfo() {
        this.targetPort = 443;
        this.target = "localhost";
        this.protocol = "https";
    }

    public GatewayTunnelInfo(String id, String gatewayId, String realm) {
        this.id = id;
        this.gatewayId = gatewayId;
        this.realm = realm;
        this.targetPort = 443;
        this.target = "localhost";
        this.protocol = "https";
    }

    public GatewayTunnelInfo(String id, String gatewayId, String realm, int targetPort, String target, String protocol) {
        this.id = id;
        this.gatewayId = gatewayId;
        this.realm = realm;
        this.targetPort = targetPort;
        this.target = target;
        this.protocol = protocol;
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

    public String getProtocol() {
        return protocol;
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

    public GatewayTunnelInfo setProtocol(String protocol) {
        this.protocol = protocol;
        return this;
    }
}
