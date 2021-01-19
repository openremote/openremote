package org.openremote.agent.protocol.tradfri.device;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openremote.agent.protocol.tradfri.device.event.EventHandler;
import org.openremote.agent.protocol.tradfri.device.event.GatewayEvent;
import org.openremote.agent.protocol.tradfri.util.ApiEndpoint;
import org.openremote.agent.protocol.tradfri.util.CoapClient;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * The class that observes an IKEA TRÅDFRI gateway to automagically detect changes
 */
public class GatewayObserver extends Observer {

    /**
     * The IKEA TRÅDFRI gateway to observe
     */
    private Gateway gateway;

    /**
     * A cache of the devices registered to the IKEA TRÅDFRI gateway
     */
    private HashMap<Integer, Device> devices;

    /**
     * An object mapper used for mapping JSON responses from the IKEA TRÅDFRI gateway to Java classes
     */
    private ObjectMapper objectMapper;

    /**
     * Construct the GatewayObserver class
     * @param gateway The IKEA TRÅDFRI gateway to observe
     * @param coapClient A CoAP client that can be used to communicate with the device using the IKEA TRÅDFRI gateway
     */
    public GatewayObserver(Gateway gateway, CoapClient coapClient) {
        super(ApiEndpoint.getUri(ApiEndpoint.DEVICES), coapClient);
        this.gateway = gateway;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Start observing the gateway to automagically detect changes
     * @return True if successfully started observing, false if not
     */
    @Override
    public boolean start(){
        Device[] devices = gateway.getDevices();
        this.devices = new HashMap<>();
        for(Device device: devices){
            this.devices.put(device.getInstanceId(), device);
        }
        return super.start();
    }

    /**
     * Handles a new response from the CoAP client and calls the appropriate event handlers for the IKEA TRÅDFRI gateway
     * @param payload The payload of the response to the CoAP request
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void callEventHandlers(String payload) {
        try {
            int[] deviceIds = objectMapper.readValue(payload, int[].class);
            ArrayList<EventHandler<?>> called = new ArrayList<>();
            GatewayEvent event = new GatewayEvent(gateway);
            for (EventHandler<?> eventHandler : gateway.getEventHandlers()) {
                if (eventHandler.getEventType().isAssignableFrom(event.getClass()) && !called.contains(eventHandler)) {
                    ((EventHandler)eventHandler).handle(event);
                    called.add(eventHandler);
                }
            }
        } catch (JsonProcessingException ignored) {
        }
    }



}
