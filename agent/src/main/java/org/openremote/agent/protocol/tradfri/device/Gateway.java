package org.openremote.agent.protocol.tradfri.device;

import org.openremote.agent.protocol.tradfri.device.event.EventHandler;
import org.openremote.agent.protocol.tradfri.payload.AuthenticateRequest;
import org.openremote.agent.protocol.tradfri.payload.AuthenticateResponse;
import org.openremote.agent.protocol.tradfri.payload.DeviceResponse;
import org.openremote.agent.protocol.tradfri.util.ApiEndpoint;
import org.openremote.agent.protocol.tradfri.util.CoapClient;
import org.openremote.agent.protocol.tradfri.util.Credentials;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * The class that is used to communicate with the IKEA TRÅDFRI gateway
 */
public class Gateway {

    /**
     * A CoAP client that can be used to communicate with the IKEA TRÅDFRI gateway
     */
    private CoapClient coapClient;

    /**
     * The observer that observes the IKEA TRÅDFRI gateway to automagically detect changes
     */
    private GatewayObserver observer;

    /**
     * The event handlers registered for the device
     */
    private final List<EventHandler<?>> eventHandlers;

    /**
     * Construct the Gateway class
     * @param ip The IP-address of the IKEA TRÅDFRI gateway
     */
    public Gateway(String ip) {
        ApiEndpoint.setGatewayIp(ip);
        coapClient = new CoapClient();
        eventHandlers = new ArrayList<>();
    }

    /**
     * Connect and authenticate to the IKEA TRÅDFRI gateway using a security code
     * @param securityCode The security code of the IKEA TRÅDFRI gateway
     * @return Credentials that can be used to authenticate to the IKEA TRÅDFRI gateway
     */
    public Credentials connect(String securityCode) {
        String identity = RandomStringUtils.randomAlphanumeric(16);
        AuthenticateRequest request = new AuthenticateRequest();
        request.setIdentity(identity);
        setCredentials("Client_identity", securityCode);
        AuthenticateResponse response = coapClient.post(ApiEndpoint.getUri(ApiEndpoint.AUTHENTICATE), request, AuthenticateResponse.class);
        if(response == null) return null;
        Credentials credentials = new Credentials(identity, response.getPresharedKey());
        setCredentials(credentials);
        return credentials;
    }

    /**
     * Connect and authenticate to the IKEA TRÅDFRI gateway using credentials
     * @param credentials The credentials that can be used to authenticate to the IKEA TRÅDFRI gateway
     * @return Credentials that can be used to authenticate to the IKEA TRÅDFRI gateway
     */
    public Credentials connect(Credentials credentials){
        setCredentials(credentials);
        return credentials;
    }

    /**
     * Change the credentials used to communicate with the IKEA TRÅDFRI gateway
     * @param credentials The new credentials that can be used to authenticate to the IKEA TRÅDFRI gateway
     */
    public void setCredentials(Credentials credentials){
        coapClient.setCredentials(credentials);
    }

    /**
     * Change the credentials used to communicate with the IKEA TRÅDFRI gateway
     * @param identity The new identity that can be used to authenticate to the IKEA TRÅDFRI gateway
     * @param key The new key that can be used to authenticate to the IKEA TRÅDFRI gateway
     */
    public void setCredentials(String identity, String key){
        Credentials credentials = new Credentials(identity, key);
        setCredentials(credentials);
    }

    /**
     * Get the credentials used to communicate with the IKEA TRÅDFRI gateway
     * @return The credentials that can be used to authenticate to the IKEA TRÅDFRI gateway
     */
    public Credentials getCredentials(){
        return coapClient.getCredentials();
    }

    /**
     * Get timeout for connections to the IKEA TRÅDFRI gateway (in milliseconds)
     * @return The timeout for connections to the IKEA TRÅDFRI gateway (in milliseconds)
     */
    public long getTimeout() {
        return coapClient.getTimeout();
    }

    /**
     * Change the timeout for connections to the IKEA TRÅDFRI gateway (in milliseconds)
     * @param timeout The new timeout for connections to the IKEA TRÅDFRI gateway (in milliseconds)
     */
    public void setTimeout(long timeout){
        coapClient.setTimeout(timeout);
    }

    /**
     * Get the ids of the devices registered to the IKEA TRÅDFRI gateway
     * @return An array of the ids of the devices registered to the IKEA TRÅDFRI gateway
     */
    public int[] getDeviceIds(){
        return coapClient.get(ApiEndpoint.getUri(ApiEndpoint.DEVICES), int[].class);
    }

    /**
     * Get the a device registered to the IKEA TRÅDFRI gateway
     * @param id The id of a device registered to the IKEA TRÅDFRI gateway
     * @return The device with the provided id
     */
    public Device getDevice(int id){
        DeviceResponse response = coapClient.get(ApiEndpoint.getUri(ApiEndpoint.DEVICES, String.valueOf(id)), DeviceResponse.class);
        if(response == null){
            return null;
        }else if(response.getLightProperties() != null && response.getLightProperties().length > 0){
            return new Light(response.getName(), response.getCreationDate(), response.getInstanceId(), response.getDeviceInfo(), response.getLightProperties()[0], coapClient);
        }else if(response.getPlugProperties() != null && response.getPlugProperties().length > 0){
            return new Plug(response.getName(), response.getCreationDate(), response.getInstanceId(), response.getDeviceInfo(), response.getPlugProperties()[0], coapClient);
        }else{
            return new Device(response.getName(), response.getCreationDate(), response.getInstanceId(), response.getDeviceInfo(), coapClient);
        }
    }

    /**
     * Get the devices registered to the IKEA TRÅDFRI gateway
     * @return An array of the devices registered to the IKEA TRÅDFRI gateway
     */
    public Device[] getDevices(){
        ArrayList<Device> deviceList = new ArrayList<>();
        int[] deviceIds = getDeviceIds();
        if(deviceIds == null) return null;
        for(int deviceId: deviceIds){
            Device device = getDevice(deviceId);
            deviceList.add(device);
        }
        Device[] devices = new Device[deviceList.size()];
        deviceList.toArray(devices);
        return devices;
    }

    /**
     * Enable observe to automagically detect changes to the device
     * @return True if successfully enabled observe, false if not
     */
    public boolean enableObserve() {
        if(observer == null) observer = new GatewayObserver(this, this.coapClient);
        return observer.start();
    }

    /**
     * Disable observe
     * @return True if successfully disabled observe, false if not
     */
    public boolean disableObserve() {
        if(observer == null) return false;
        return observer.stop();
    }

    /**
     * Get a list of event handlers for the IKEA TRÅDFRI gateway
     * @return A list of event handlers for the IKEA TRÅDFRI gateway
     */
    public List<EventHandler<?>> getEventHandlers(){
        return eventHandlers;
    }

    /**
     * Add an event handler to the IKEA TRÅDFRI gateway
     * @param eventHandler The event handler to add to the IKEA TRÅDFRI gateway
     *                     @since 1.0.0
     */
    public void addEventHandler(EventHandler<?> eventHandler){
        this.eventHandlers.add(eventHandler);
    }

    /**
     * Remove an event handler from the IKEA TRÅDFRI gateway
     * @param eventHandler The event handler to remove from the IKEA TRÅDFRI gateway
     */
    public void removeEventHandler(EventHandler<?> eventHandler){
        this.eventHandlers.remove(eventHandler);
    }

}
