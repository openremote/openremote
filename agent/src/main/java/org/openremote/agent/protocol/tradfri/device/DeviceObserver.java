package org.openremote.agent.protocol.tradfri.device;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openremote.agent.protocol.tradfri.device.event.*;
import org.openremote.agent.protocol.tradfri.payload.DeviceResponse;
import org.openremote.agent.protocol.tradfri.util.CoapClient;

import java.util.ArrayList;

/**
 * The class that observes a device to automagically detect changes
 * @author Stijn Groenen
 * @version 1.0.0
 */
public class DeviceObserver extends Observer {

    /**
     * The device to observe
     */
    private Device device;

    /**
     * An object mapper used for mapping JSON responses from the IKEA TRÅDFRI gateway to Java classes
     */
    private ObjectMapper objectMapper;

    /**
     * Construct the DeviceObserver class
     * @param device The device to observe
     * @param coapClient A CoAP client that can be used to communicate with the device using the IKEA TRÅDFRI gateway
     * @since 1.0.0
     */
    public DeviceObserver(Device device, CoapClient coapClient) {
        super(device.getEndpoint(), coapClient);
        this.device = device;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Handles a new response from the CoAP client and calls the appropriate event handlers for the device
     * @param payload The payload of the response to the CoAP request
     * @since 1.0.0
     */
    @Override
    public void callEventHandlers(String payload) {
        try {
            DeviceResponse response = objectMapper.readValue(payload, DeviceResponse.class);
            ArrayList<DeviceEvent> events = new ArrayList<>();
            ArrayList<EventHandler> called = new ArrayList<>();
            if(device.isLight()){
                LightProperties oldProperties = (LightProperties) device.getProperties();
                if(response.getLightProperties() != null && response.getLightProperties().length > 0) device.setProperties(response.getLightProperties()[0]);
                LightProperties newProperties = (LightProperties) device.getProperties();
                events.add(new LightEvent(device.toLight()));
                ArrayList<DeviceEvent> changeEvents = new ArrayList<>();
                if(checkChanges(oldProperties.getOn(), newProperties.getOn())) changeEvents.add(new LightChangeOnEvent(device.toLight(), oldProperties, newProperties));
                if(checkChanges(oldProperties.getBrightness(), newProperties.getBrightness())) changeEvents.add(new LightChangeBrightnessEvent(device.toLight(), oldProperties, newProperties));
                if(checkChanges(oldProperties.getColourHex(), newProperties.getColourHex())) changeEvents.add(new LightChangeColourHexEvent(device.toLight(), oldProperties, newProperties));
                if(checkChanges(oldProperties.getHue(), newProperties.getHue())) changeEvents.add(new LightChangeHueEvent(device.toLight(), oldProperties, newProperties));
                if(checkChanges(oldProperties.getSaturation(), newProperties.getSaturation())) changeEvents.add(new LightChangeSaturationEvent(device.toLight(), oldProperties, newProperties));
                if(checkChanges(oldProperties.getColourX(), newProperties.getColourX())) changeEvents.add(new LightChangeColourXEvent(device.toLight(), oldProperties, newProperties));
                if(checkChanges(oldProperties.getColourY(), newProperties.getColourY())) changeEvents.add(new LightChangeColourYEvent(device.toLight(), oldProperties, newProperties));
                if(checkChanges(oldProperties.getColourTemperature(), newProperties.getColourTemperature())) changeEvents.add(new LightChangeColourTemperatureEvent(device.toLight(), oldProperties, newProperties));
                if(changeEvents.size() > 0){
                    events.add(new LightChangeEvent(device.toLight(), oldProperties, newProperties));
                    events.addAll(changeEvents);
                }
            }else if(device.isPlug()){
                PlugProperties oldProperties = (PlugProperties) device.getProperties();
                if(response.getPlugProperties() != null && response.getPlugProperties().length > 0) device.setProperties(response.getPlugProperties()[0]);
                PlugProperties newProperties = (PlugProperties) device.getProperties();
                events.add(new PlugEvent(device.toPlug()));
                if(checkChanges(oldProperties.getOn(), newProperties.getOn())){
                    events.add(new PlugChangeEvent(device.toPlug(), oldProperties, newProperties));
                    events.add(new PlugChangeOnEvent(device.toPlug(), oldProperties, newProperties));
                }
            }else if(device.isRemote()){
                events.add(new RemoteEvent(device.toRemote()));
            }else if(device.isMotionSensor()){
                events.add(new MotionSensorEvent(device.toMotionSensor()));
            }
            for(EventHandler eventHandler: device.getEventHandlers()){
                for(DeviceEvent event: events){
                    if(eventHandler.getEventType().isAssignableFrom(event.getClass()) && !called.contains(eventHandler)){
                        eventHandler.handle(event);
                        called.add(eventHandler);
                    }
                }
            }
        } catch (JsonProcessingException ignored) { }
    }

}
