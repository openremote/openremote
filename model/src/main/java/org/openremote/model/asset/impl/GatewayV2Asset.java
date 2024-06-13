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
package org.openremote.model.asset.impl;

import jakarta.persistence.Entity;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueType;

import java.util.Optional;

@Entity
public class GatewayV2Asset extends Asset<GatewayV2Asset> {

    public static final AttributeDescriptor<String> CLIENT_ID = new AttributeDescriptor<>("clientId", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));
    public static final AttributeDescriptor<String> CLIENT_SECRET = new AttributeDescriptor<>("clientSecret", ValueType.UUID);
    public static final AttributeDescriptor<ConnectionStatus> STATUS = new AttributeDescriptor<>("gatewayStatus", ValueType.CONNECTION_STATUS, new MetaItem<>(MetaItemType.READ_ONLY));
    public static final AttributeDescriptor<Boolean> DISABLED = new AttributeDescriptor<>("disabled", ValueType.BOOLEAN);
    public static final AssetDescriptor<GatewayV2Asset> DESCRIPTOR = new AssetDescriptor<>("router-wireless", "5a20cc", GatewayV2Asset.class);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected GatewayV2Asset() {
    }

    public GatewayV2Asset(String name) {
        super(name);
    }

    public Optional<String> getClientId() {
        return getAttributes().getValue(CLIENT_ID);
    }

    public GatewayV2Asset setClientId(String clientId) {
        getAttributes().getOrCreate(CLIENT_ID).setValue(clientId);
        return this;
    }

    public Optional<String> getClientSecret() {
        return getAttributes().getValue(CLIENT_SECRET);
    }

    public GatewayV2Asset setClientSecret(String clientSecret) {
        getAttributes().getOrCreate(CLIENT_SECRET).setValue(clientSecret);
        return this;
    }

    public Optional<ConnectionStatus> getGatewayStatus() {
        return getAttributes().getValue(STATUS);
    }

    public GatewayV2Asset setGatewayStatus(ConnectionStatus connectionStatus) {
        getAttributes().getOrCreate(STATUS).setValue(connectionStatus);
        return this;
    }

    public Optional<Boolean> getDisabled() {
        return getAttributes().getValue(DISABLED);
    }

    public GatewayV2Asset setDisabled(Boolean disabled) {
        getAttributes().getOrCreate(DISABLED).setValue(disabled);
        return this;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        GatewayV2Asset other = (GatewayV2Asset) obj;
        return id.equals(other.id);
    }
}
