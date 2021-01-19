package org.openremote.agent.protocol.tradfri.device;

import org.openremote.agent.protocol.tradfri.device.event.EventHandler;
import org.openremote.agent.protocol.tradfri.util.ApiEndpoint;
import org.openremote.agent.protocol.tradfri.util.CoapClient;

import java.util.ArrayList;
import java.util.List;

/**
 * The class that represents an IKEA TRÅDFRI device
 */
public class Device {

    /**
     * The name of the device
     */
    private String name;

    /**
     * The creation date of the device
     */
    private Long creationDate;

    /**
     * The instance id of the device
     */
    private Integer instanceId;

    /**
     * The information of the device
     */
    private DeviceInfo deviceInfo;

    /**
     * The properties of the device
     */
    private DeviceProperties properties;

    /**
     * A CoAP client that can be used to communicate with the device using the IKEA TRÅDFRI gateway
     */
    protected CoapClient coapClient;

    /**
     * The observer that observes the device to automagically detect changes
     */
    private DeviceObserver observer;

    /**
     * The event handlers registered for the device
     */
    private final List<EventHandler<?>> eventHandlers;

    /**
     * Construct the Device class
     * @param name The name of the device
     * @param creationDate The creation date of the device
     * @param instanceId The instance id of the device
     * @param deviceInfo The information of the device
     * @param coapClient A CoAP client that can be used to communicate with the device using the IKEA TRÅDFRI gateway
     */
    public Device(String name, Long creationDate, Integer instanceId, DeviceInfo deviceInfo, CoapClient coapClient){
        this.name = name;
        this.creationDate = creationDate;
        this.instanceId = instanceId;
        this.deviceInfo = deviceInfo;
        this.coapClient = coapClient;
        this.eventHandlers = new ArrayList<>();
    }

    /**
     * Get the name of the device
     * @return The name of the device
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get the creation date of the device
     * @return The creation date of the device
     */
    public Long getCreationDate() {
        return this.creationDate;
    }

    /**
     * Get the instance id of the device
     * @return The instance id of the device
     */
    public Integer getInstanceId() {
        return this.instanceId;
    }

    /**
     * Get the information of the device
     * @return The information of the device
     */
    public DeviceInfo getDeviceInfo() {
        return this.deviceInfo;
    }

    /**
     * Set the name of the device
     * @param name The name of the device
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Set the creation date of the device
     * @param creationDate The creation date of the device
     */
    public void setCreationDate(Long creationDate) {
        this.creationDate = creationDate;
    }

    /**
     * Set the instance id of the device
     * @param instanceId The instance id of the device
     */
    public void setInstanceId(Integer instanceId) {
        this.instanceId = instanceId;
    }

    /**
     * Get the properties of the device
     * @return The properties of the device
     */
    public DeviceProperties getProperties(){
        return this.properties;
    }

    /**
     * Set the properties of the device
     * @param properties The properties of the device
     */
    public void setProperties(DeviceProperties properties){
        this.properties = properties;
    }

    /**
     * Get the CoAP endpoint of the device
     * @return The CoAP endpoint of the device
     */
    public String getEndpoint(){
        return ApiEndpoint.getUri(ApiEndpoint.DEVICES, String.valueOf(getInstanceId()));
    }

    /**
     * Enable observe to automagically detect changes to the device
     * @return True if successfully enabled observe, false if not
     */
    public boolean enableObserve() {
        if(observer == null) observer = new DeviceObserver(this, this.coapClient);
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
     * Get a list of event handlers for the device
     * @return A list of event handlers for the device
     */
    public List<EventHandler<?>> getEventHandlers(){
        return eventHandlers;
    }

    /**
     * Add an event handler to the device
     * @param eventHandler The event handler to add to the device
     */
    public void addEventHandler(EventHandler<?> eventHandler){
        this.eventHandlers.add(eventHandler);
    }

    /**
     * Remove an event handler from the device
     * @param eventHandler The event handler to remove from the device
     */
    public void removeEventHandler(EventHandler<?> eventHandler){
        this.eventHandlers.remove(eventHandler);
    }

    /**
     * Check if the device is a {@link Light}
     * @return True if the device is a {@link Light}, false if not
     */
    public boolean isLight(){
        return this instanceof Light;
    }

    /**
     * Convert the device to the {@link Light} class
     * @return The device as {@link Light}
     */
    public Light toLight(){
        if(isLight()) return (Light) this;
        return null;
    }

    /**
     * Check if the device is a {@link Plug}
     * @return True if the device is a {@link Plug}, false if not
     */
    public boolean isPlug(){
        return this instanceof Plug;
    }

    /**
     * Convert the device to the {@link Plug} class
     * @return The device as {@link Plug}
     */
    public Plug toPlug(){
        if(isPlug()) return (Plug) this;
        return null;
    }
}
