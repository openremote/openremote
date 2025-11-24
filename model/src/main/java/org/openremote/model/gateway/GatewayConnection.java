/*
 * Copyright 2020, OpenRemote Inc.
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
package org.openremote.model.gateway;

import com.fasterxml.jackson.annotation.JsonCreator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;

@Entity
@Table(name = "GATEWAY_CONNECTION")
public class GatewayConnection {

    @Id
    @Column(name = "LOCAL_REALM", nullable = false)
    protected String localRealm;

    @NotNull(message = "{GatewayConnection.host.NotNull}")
    @Size(min = 1, max = 255, message = "{GatewayConnection.host.Size}")
    @Column(name = "HOST", nullable = false)
    protected String host;

    @Min(value = 1, message = "{GatewayConnection.port.Range}")
    @Max(value = 65536, message = "{GatewayConnection.port.Range}")
    @Column(name = "PORT")
    protected Integer port;

    @Column(name = "REALM", nullable = false)
    protected String realm;

    @NotNull(message = "{GatewayConnection.clientId.NotNull}")
    @Size(min = 1, max = 255, message = "{GatewayConnection.clientId.Size}")
    @Column(name = "CLIENT_ID", nullable = false, length = 36)
    protected String clientId;

    @NotNull(message = "{GatewayConnection.clientSecret.NotNull}")
    @Size(min = 36, max = 36, message = "{GatewayConnection.clientSecret.Size}")
    @Column(name = "CLIENT_SECRET", nullable = false, length = 36)
    protected String clientSecret;

    @Column(name = "SECURED")
    protected Boolean secured;

    @Column(name = "DISABLED")
    protected boolean disabled;

    /**
     * Filters are applied in order and the first to match the {@link org.openremote.model.attribute.AttributeEvent} will
     * be applied; if a catch all filter is to be used (i.e. {@link GatewayAttributeFilter#matcher == null}) it should
     * be last in the list.
     */
    @Column(name = "ATTRIBUTE_FILTERS")
    @JdbcTypeCode(SqlTypes.JSON)
    protected List<GatewayAttributeFilter> attributeFilters;

    /**
     * A map of {@link GatewayAssetSyncRule}s where the key should be an {@link org.openremote.model.asset.Asset} type
     * to which the rules should be applied, to apply to all asset types use the * wildcard.
     */
    @Column(name = "SYNC_RULES")
    @JdbcTypeCode(SqlTypes.JSON)
    protected Map<String, GatewayAssetSyncRule> assetSyncRules;

    /**
     * For JPA
     */
    protected GatewayConnection() {
    }

    @JsonCreator
    public GatewayConnection(
        String localRealm,
        String host,
        Integer port,
        String realm,
        String clientId,
        String clientSecret,
        Boolean secured,
        List<GatewayAttributeFilter> attributeFilters,
        Map<String, GatewayAssetSyncRule> assetSyncRules,
        boolean disabled) {
        this.localRealm = localRealm;
        this.host = host;
        this.port = port;
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.secured = secured;
        this.disabled = disabled;
        this.attributeFilters = attributeFilters;
        this.assetSyncRules = assetSyncRules;
    }

    public GatewayConnection(
        String host,
        Integer port,
        String realm,
        String clientId,
        String clientSecret,
        Boolean secured,
        boolean disabled) {
        this.host = host;
        this.port = port;
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.secured = secured;
        this.disabled = disabled;
    }

    public String getLocalRealm() {
        return localRealm;
    }

    public void setLocalRealm(String localRealm) {
        this.localRealm = localRealm;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public boolean isSecured() {
        return secured == null || secured;
    }

    public void setSecured(boolean secured) {
        this.secured = secured;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public List<GatewayAttributeFilter> getAttributeFilters() {
        return attributeFilters;
    }

    public GatewayConnection setAttributeFilters(List<GatewayAttributeFilter> attributeFilters) {
        this.attributeFilters = attributeFilters;
        return this;
    }

    public Map<String, GatewayAssetSyncRule> getAssetSyncRules() {
        return assetSyncRules;
    }

    public GatewayConnection setAssetSyncRules(Map<String, GatewayAssetSyncRule> assetSyncRules) {
        this.assetSyncRules = assetSyncRules;
        return this;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "localRealm='" + localRealm + '\'' +
            ", host='" + host + '\'' +
            ", port=" + port +
            ", realm='" + realm + '\'' +
            ", secured=" + secured +
            ", attributeFilters=" + (attributeFilters != null && !attributeFilters.isEmpty()) +
            ", assetSyncRules=" + assetSyncRules +
            ", disabled=" + disabled +
            '}';
    }
}
