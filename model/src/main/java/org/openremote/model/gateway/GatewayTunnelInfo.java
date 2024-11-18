package org.openremote.model.gateway;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openremote.model.util.UniqueIdentifierGenerator;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

public class GatewayTunnelInfo {

    public enum Type {
        HTTPS,
        HTTP,
        TCP
    }

    protected String gatewayId;
    protected String realm;
    protected int targetPort = 443;
    protected String target = "localhost";
    protected Integer assignedPort = null;
    protected String hostname;
    protected Type type = Type.HTTPS;
    protected Instant autoCloseTime;

    @JsonCreator
    public GatewayTunnelInfo(String realm, String gatewayId) {
        this.realm = realm;
        this.gatewayId = gatewayId;
    }

    public GatewayTunnelInfo(String realm, String gatewayId, Type type, String target, int targetPort) {
        this.gatewayId = gatewayId;
        this.realm = realm;
        this.targetPort = targetPort;
        this.target = target;
        this.type = type;
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

    public Type getType() {
        return type;
    }

    public Integer getAssignedPort() {
        return assignedPort;
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

    public GatewayTunnelInfo setType(Type type) {
        this.type = type;
        return this;
    }

    public GatewayTunnelInfo setAssignedPort(Integer assignedPort) {
        this.assignedPort = assignedPort;
        return this;
    }

    public String getHostname() {
        return hostname;
    }

    public GatewayTunnelInfo setHostname(String hostname) {
        this.hostname = hostname;
        return this;
    }

    public Instant getAutoCloseTime() {
        return autoCloseTime;
    }

    public GatewayTunnelInfo setAutoCloseTime(Instant autoCloseTime) {
        this.autoCloseTime = autoCloseTime;
        return this;
    }

    @JsonProperty
    public String getId() {
        String seed = gatewayId + target + targetPort;
        return "gw-" + UniqueIdentifierGenerator.generateId(seed).toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GatewayTunnelInfo that = (GatewayTunnelInfo) o;
        return targetPort == that.targetPort && Objects.equals(gatewayId, that.gatewayId) && Objects.equals(target, that.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gatewayId, targetPort, target);
    }

    @Override
    public String toString() {
        return GatewayTunnelInfo.class.getSimpleName() + "{" +
            "id='" + getId() + '\'' +
            ", gatewayId='" + gatewayId + '\'' +
            ", realm='" + realm + '\'' +
            ", targetPort=" + targetPort +
            ", target='" + target + '\'' +
            ", type='" + type + '\'' +
            ", assignedPort=" + assignedPort +
            '}';
    }
}
