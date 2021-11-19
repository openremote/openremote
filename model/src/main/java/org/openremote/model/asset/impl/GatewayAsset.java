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

import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueType;

import javax.persistence.Entity;
import java.util.Optional;

@Entity
public class GatewayAsset extends Asset<GatewayAsset> {

    public static final AttributeDescriptor<String> CLIENT_ID = new AttributeDescriptor<>("clientId", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));
    public static final AttributeDescriptor<String> CLIENT_SECRET = new AttributeDescriptor<>("clientSecret", ValueType.UUID);
    public static final AttributeDescriptor<ConnectionStatus> STATUS = new AttributeDescriptor<>("gatewayStatus", ValueType.CONNECTION_STATUS, new MetaItem<>(MetaItemType.READ_ONLY));
    public static final AttributeDescriptor<Boolean> DISABLED = new AttributeDescriptor<>("disabled", ValueType.BOOLEAN);

    public static final AssetDescriptor<GatewayAsset> DESCRIPTOR = new AssetDescriptor<>("router-wireless", null, GatewayAsset.class);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected GatewayAsset() {
    }

    public GatewayAsset(String name) {
        super(name);
    }

    public Optional<String> getClientId() {
        return getAttributes().getValue(CLIENT_ID);
    }

    public GatewayAsset setClientId(String clientId) {
        getAttributes().getOrCreate(CLIENT_ID).setValue(clientId);
        return this;
    }

    public Optional<String> getClientSecret() {
        return getAttributes().getValue(CLIENT_SECRET);
    }

    public GatewayAsset setClientSecret(String clientSecret) {
        getAttributes().getOrCreate(CLIENT_SECRET).setValue(clientSecret);
        return this;
    }

    public Optional<ConnectionStatus> getGatewayStatus() {
        return getAttributes().getValue(STATUS);
    }

    public GatewayAsset setGatewayStatus(ConnectionStatus connectionStatus) {
        getAttributes().getOrCreate(STATUS).setValue(connectionStatus);
        return this;
    }

    public Optional<Boolean> getDisabled() {
        return getAttributes().getValue(DISABLED);
    }

    public GatewayAsset setDisabled(Boolean disabled) {
        getAttributes().getOrCreate(DISABLED).setValue(disabled);
        return this;
    }
}
