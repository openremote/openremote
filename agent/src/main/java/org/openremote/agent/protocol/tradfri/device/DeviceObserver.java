package org.openremote.agent.protocol.tradfri.device;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openremote.agent.protocol.tradfri.device.event.*;
import org.openremote.agent.protocol.tradfri.payload.DeviceResponse;
import org.openremote.agent.protocol.tradfri.util.CoapClient;
import org.openremote.model.util.ValueUtil;

import java.util.ArrayList;

/**
 * The class that observes a device to automagically detect changes
 */
public class DeviceObserver extends Observer {

    /**
     * The device to observe
     */
    private final Device device;

    /**
     * An object mapper used for mapping JSON responses from the IKEA TRÅDFRI gateway to Java classes
     */
    private final ObjectMapper objectMapper = ValueUtil.JSON;

    /**
     * Construct the DeviceObserver class
     * @param device The device to observe
     * @param coapClient A CoAP client that can be used to communicate with the device using the IKEA TRÅDFRI gateway
     */
    public DeviceObserver(Device device, CoapClient coapClient) {
        super(device.getEndpoint(), coapClient);
        this.device = device;
    }

    /**
     * Handles a new response from the CoAP client and calls the appropriate event handlers for the device
     * @param payload The payload of the response to the CoAP request
     */
    @SuppressWarnings("unchecked")
    @Override
    public void callEventHandlers(String payload) {
        try {
            DeviceResponse response = objectMapper.readValue(payload, DeviceResponse.class);
            ArrayList<DeviceEvent> events = new ArrayList<>();
            ArrayList<EventHandler<?>> called = new ArrayList<>();
            if (device.isLight()){
                LightProperties oldProperties = (LightProperties) device.getProperties();
                if(response.getLightProperties() != null && response.getLightProperties().length > 0) device.setProperties(response.getLightProperties()[0]);
                LightProperties newProperties = (LightProperties) device.getProperties();
                events.add(new LightEvent(device.toLight()));
                ArrayList<DeviceEvent> changeEvents = new ArrayList<>();
                if (checkChanges(oldProperties.getOn(), newProperties.getOn())) changeEvents.add(new LightChangeOnEvent(device.toLight(), oldProperties, newProperties));
                if (checkChanges(oldProperties.getBrightness(), newProperties.getBrightness())) changeEvents.add(new LightChangeBrightnessEvent(device.toLight(), oldProperties, newProperties));
                if (checkChanges(oldProperties.getColourX(), newProperties.getColourX()) || checkChanges(oldProperties.getColourY(), newProperties.getColourY()) || checkChanges(oldProperties.getHue(), newProperties.getHue()) || checkChanges(oldProperties.getSaturation(), newProperties.getSaturation())) changeEvents.add(new LightChangeColourEvent(device.toLight(), oldProperties, newProperties));
                if (checkChanges(oldProperties.getColourTemperature(), newProperties.getColourTemperature())) changeEvents.add(new LightChangeColourTemperatureEvent(device.toLight(), oldProperties, newProperties));
                if (changeEvents.size() > 0){
                    events.add(new LightChangeEvent(device.toLight(), oldProperties, newProperties));
                    events.addAll(changeEvents);
                }
            } else if (device.isPlug()) {
                PlugProperties oldProperties = (PlugProperties) device.getProperties();
                if(response.getPlugProperties() != null && response.getPlugProperties().length > 0) device.setProperties(response.getPlugProperties()[0]);
                PlugProperties newProperties = (PlugProperties) device.getProperties();
                events.add(new PlugEvent(device.toPlug()));
                if(checkChanges(oldProperties.getOn(), newProperties.getOn())){
                    events.add(new PlugChangeEvent(device.toPlug(), oldProperties, newProperties));
                    events.add(new PlugChangeOnEvent(device.toPlug(), oldProperties, newProperties));
                }
            }
            for (EventHandler<?> eventHandler: device.getEventHandlers()) {
                for (DeviceEvent event: events) {
                    if (eventHandler.getEventType().isAssignableFrom(event.getClass()) && !called.contains(eventHandler)) {
                        ((EventHandler<DeviceEvent>)eventHandler).handle(event);
                        called.add(eventHandler);
                    }
                }
            }
        } catch (JsonProcessingException ignored) { }
    }

}
