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

import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueDescriptor;

import javax.persistence.Entity;
import java.util.Optional;

@SuppressWarnings("unchecked")
@Entity
public class ElectricityChargerAsset extends ElectricityAsset<ElectricityChargerAsset> {

    public enum ConnectorType {
        YAZAKI,
        MENNEKES,
        LE_GRAND,
        CHADEMO,
        COMBO,
        SCHUKO
    }

    public static final ValueDescriptor<ConnectorType> CONNECTOR_TYPE_VALUE = new ValueDescriptor<>("connectorType", ConnectorType.class);

    public static final AttributeDescriptor<ConnectorType> CONNECTOR_TYPE = new AttributeDescriptor<>("connectorType", CONNECTOR_TYPE_VALUE);

    public static final AssetDescriptor<ElectricityChargerAsset> DESCRIPTOR = new AssetDescriptor<>("ev-station", "8A293D", ElectricityChargerAsset.class);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    ElectricityChargerAsset() {
        this(null);
    }

    public ElectricityChargerAsset(String name) {
        super(name);
    }

    public Optional<ConnectorType> getConnectorType() {
        return getAttributes().getValue(CONNECTOR_TYPE);
    }

    
    public ElectricityChargerAsset setConnectorType(ConnectorType value) {
        getAttributes().getOrCreate(CONNECTOR_TYPE).setValue(value);
        return this;
    }
}
