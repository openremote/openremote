/*
 * Copyright 2025, OpenRemote Inc.
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
package org.openremote.setup.integration.model.asset;

import jakarta.persistence.Entity;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueType;

import java.util.Map;
import java.util.Optional;

import static org.openremote.model.Constants.UNITS_CELSIUS;

@Entity
public class ChirpStackTestAsset extends Asset<ChirpStackTestAsset> {

    public static final AttributeDescriptor<Double> TEMPERATURE = new AttributeDescriptor<>("temperature", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 2,
                "valueFilterJsonPath", "$.object.Temperature"
            ));
        }})
    ).withUnits(UNITS_CELSIUS);
    public static final AttributeDescriptor<Double> RELATIVE_HUMIDITY = new AttributeDescriptor<>("relativeHumidity", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 2,
                "valueFilterJsonPath", "$.object.Humidity"
            ));
        }})
    );
    public static final AttributeDescriptor<Boolean> SWITCH = new AttributeDescriptor<>("switch", ValueType.BOOLEAN,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "downlinkPort", 4,
                "writeValueConverter", new ValueType.ObjectMap() {{
                    putAll(Map.of(
                        "TRUE", "DAE=",
                        "FALSE", "DAA="
                    ));
                }}
            ));
        }})
    );

    public static final AttributeDescriptor<String> DEV_EUI = new AttributeDescriptor<>("devEUI", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));
    public static final AttributeDescriptor<String> VENDOR_ID = new AttributeDescriptor<>("vendorId", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));
    public static final AttributeDescriptor<String> MODEL_ID = new AttributeDescriptor<>("modelId", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));
    public static final AttributeDescriptor<String> FIRMWARE_VERSION = new AttributeDescriptor<>("firmwareVersion", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));
    public static final AttributeDescriptor<Boolean> SUPPORTS_CLASS_C = new AttributeDescriptor<>("supportsClassC", ValueType.BOOLEAN, new MetaItem<>(MetaItemType.READ_ONLY));

    public static final AssetDescriptor<ChirpStackTestAsset> DESCRIPTOR = new AssetDescriptor<>("molecule-co2", "f18546", ChirpStackTestAsset.class);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected ChirpStackTestAsset() {
    }

    public ChirpStackTestAsset(String name) {
        super(name);
    }

    public Optional<Double> getTemperature() {
        return getAttributes().getValue(TEMPERATURE);
    }

    public Optional<Double> getRelativeHumidity() {
        return getAttributes().getValue(RELATIVE_HUMIDITY);
    }

}
