package org.openremote.agent.protocol.tradfri;

import org.openremote.agent.protocol.tradfri.device.Device;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeMap;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueType;

import java.util.Optional;
import java.util.function.Consumer;

public interface TradfriAsset {

    AttributeDescriptor<Integer> DEVICE_ID = new AttributeDescriptor<>("deviceId", ValueType.INTEGER);

    default Optional<Integer> getDeviceId() {
        return getAttributes().getValue(DEVICE_ID);
    }

    default void setDeviceId(Integer deviceId) {
        getAttributes().get(DEVICE_ID).orElse(new Attribute<>(DEVICE_ID)).setValue(deviceId);
    }

    String getId();

    AttributeMap getAttributes();

    void addEventHandlers(Device device, Consumer<AttributeEvent> attributeEventConsumer);

    void initialiseAttributes(Device device);
}
