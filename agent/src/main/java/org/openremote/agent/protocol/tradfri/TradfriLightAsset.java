package org.openremote.agent.protocol.tradfri;

import org.openremote.agent.protocol.tradfri.device.Device;
import org.openremote.agent.protocol.tradfri.device.Light;
import org.openremote.agent.protocol.tradfri.device.event.*;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.asset.impl.LightAsset;
import org.openremote.model.attribute.AttributeEvent;

import javax.persistence.Entity;
import java.util.function.Consumer;

@Entity
public class TradfriLightAsset extends LightAsset implements TradfriAsset {

    public static final AssetDescriptor<TradfriLightAsset> DESCRIPTOR = new AssetDescriptor<>(
        "lightbulb", "e6688a", TradfriLightAsset.class
    );

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected TradfriLightAsset() {
    }

    public TradfriLightAsset(String name) {
        super(name);
    }

    @Override
    public void addEventHandlers(Device device, Consumer<AttributeEvent> attributeEventConsumer) {
        Light light = device.toLight();
        if (light == null) {
            return;
        }
        EventHandler<LightChangeOnEvent> lightOnOffEventHandler = new EventHandler<LightChangeOnEvent>() {
            @Override
            public void handle(LightChangeOnEvent event) {
                attributeEventConsumer.accept(new AttributeEvent(getId(), ON_OFF.getName(), light.getOn()));
            }
        };

        EventHandler<LightChangeBrightnessEvent> lightBrightnessEventHandler = new EventHandler<LightChangeBrightnessEvent>() {
            @Override
            public void handle(LightChangeBrightnessEvent event) {
                attributeEventConsumer.accept(new AttributeEvent(getId(), BRIGHTNESS.getName(), convertBrightness(light.getBrightness(), true)));
            }
        };

        EventHandler<LightChangeColourEvent> lightColourChangeEventHandler = new EventHandler<LightChangeColourEvent>() {
            @Override
            public void handle(LightChangeColourEvent event) {
                attributeEventConsumer.accept(new AttributeEvent(getId(), COLOUR_RGB.getName(), light.getColourRGB()));
            }
        };

        EventHandler<LightChangeColourTemperatureEvent> lightColorTemperatureEventHandler = new EventHandler<LightChangeColourTemperatureEvent>() {
            @Override
            public void handle(LightChangeColourTemperatureEvent event) {
                attributeEventConsumer.accept(new AttributeEvent(getId(), COLOUR_TEMPERATURE.getName(), light.getColourTemperature()));
            }
        };

        light.addEventHandler(lightOnOffEventHandler);
        light.addEventHandler(lightBrightnessEventHandler);
        light.addEventHandler(lightColourChangeEventHandler);
        light.addEventHandler(lightColorTemperatureEventHandler);
    }

    @Override
    public void initialiseAttributes(Device device) {
        Light light = device.toLight();
        if (light == null) {
            return;
        }
        getAttributes().get(ON_OFF).ifPresent(attribute -> attribute.setValue(light.getOn()));

        getAttributes().get(BRIGHTNESS).ifPresent(attribute ->
            attribute.setValue(convertBrightness(light.getBrightness(), true)));

        getAttributes().get(COLOUR_RGB).ifPresent(attribute -> attribute.setValue(light.getColourRGB()));

        getAttributes().get(COLOUR_TEMPERATURE).ifPresent(attribute -> attribute.setValue(light.getColourTemperature()));
    }

    public static Integer convertBrightness(Integer value, boolean toPercentage) {
        if (value == null) {
            return null;
        }

        if (toPercentage) {
            return (int)Math.round((Double.valueOf(value) / 254d) * 100);
        }

        return (int)Math.round((Double.valueOf(value) / 100) * 254d);
    }
}
