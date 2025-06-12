package org.openremote.model.moenchengladbach;

import jakarta.persistence.Entity;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueType;

import java.util.Map;
import java.util.Optional;

@Entity
public class ParkingSensorNwaveBTTags extends Asset<ParkingSensorNwaveBTTags> {
    public static final AttributeDescriptor<Boolean> OCCUPIED = new AttributeDescriptor<>("occupied", ValueType.BOOLEAN,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "valueFilterJsonPath", "$.object.occupied",
                "valueConverter", new ValueType.ObjectMap() {{
                    putAll(Map.of(
                        "TRUE", "true",
                        "FALSE", "false"
                    ));
                }}
            ));
        }})
    );
    public static final AttributeDescriptor<String> BATTERY_HEALTH = new AttributeDescriptor<>("batteryHealth", ValueType.TEXT,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 2,
                "valueFilterJsonPath", "$.object.battery_health"
            ));
        }})
    );
    public static final AttributeDescriptor<Integer> PREVIOUS_STATE_DURATION = new AttributeDescriptor<>("previousStateDuration", ValueType.INTEGER,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 10,
                "valueFilterJsonPath", "$.object.previous_state_duration"
            ));
        }})
    );
    public static final AttributeDescriptor<Integer> PREVIOUS_STATE_DURATION_ERROR = new AttributeDescriptor<>("previousStateDurationError", ValueType.POSITIVE_INTEGER,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 10,
                "valueFilterJsonPath", "$.object.previous_state_duration_error"
            ));
        }})
    );
    public static final AttributeDescriptor<Boolean> PREVIOUS_STATE_DURATION_OVERFLOW = new AttributeDescriptor<>("previousStateDurationOverflow", ValueType.BOOLEAN,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 10,
                "valueFilterJsonPath", "$.object.previous_state_duration_overflow",
                "valueConverter", new ValueType.ObjectMap() {{
                    putAll(Map.of(
                        "TRUE", "true",
                        "FALSE", "false"
                    ));
                }}
            ));
        }})
    );
    public static final AttributeDescriptor<String> TAG_ID = new AttributeDescriptor<>("tagId", ValueType.TEXT,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 10,
                "valueFilterJsonPath", "$.object.tag_id"
            ));
        }})
    );

    public static final AttributeDescriptor<String> DEV_EUI = new AttributeDescriptor<>("devEUI", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));
    public static final AttributeDescriptor<String> VENDOR_ID = new AttributeDescriptor<>("vendorId", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));
    public static final AttributeDescriptor<String> MODEL_ID = new AttributeDescriptor<>("modelId", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));
    public static final AttributeDescriptor<String> FIRMWARE_VERSION = new AttributeDescriptor<>("firmwareVersion", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));
    public static final AttributeDescriptor<Boolean> SUPPORTS_CLASS_C = new AttributeDescriptor<>("supportsClassC", ValueType.BOOLEAN, new MetaItem<>(MetaItemType.READ_ONLY));

    public static final AssetDescriptor<ParkingSensorNwaveBTTags> DESCRIPTOR = new AssetDescriptor<>("parking", "0260ae", ParkingSensorNwaveBTTags.class);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected ParkingSensorNwaveBTTags() {
    }

    public ParkingSensorNwaveBTTags(String name) {
        super(name);
    }

    public Optional<Boolean> getOccupied() {
        return getAttributes().getValue(OCCUPIED);
    }

    public Optional<String> getBatteryHealth() {
        return getAttributes().getValue(BATTERY_HEALTH);
    }

    public Optional<Integer> getPreviousStateDuration() {
        return getAttributes().getValue(PREVIOUS_STATE_DURATION);
    }

    public Optional<Integer> getPreviousStateDurationError() {
        return getAttributes().getValue(PREVIOUS_STATE_DURATION_ERROR);
    }

    public Optional<Boolean> isPreviousStateDurationOverflow() {
        return getAttributes().getValue(PREVIOUS_STATE_DURATION_OVERFLOW);
    }

    public Optional<String> getTagId() {
        return getAttributes().getValue(TAG_ID);
    }
}
