package org.openremote.agent.protocol.tradfri.device;

import org.openremote.agent.protocol.tradfri.device.event.EventHandler;
import org.openremote.agent.protocol.tradfri.util.ApiEndpoint;
import org.openremote.agent.protocol.tradfri.util.CoapClient;

import java.util.ArrayList;
import java.util.List;

/**
 * The class that represents an IKEA TRÅDFRI device
 * @author Stijn Groenen
 * @version 1.1.0
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
    private List<EventHandler> eventHandlers;

    /**
     * Construct the Device class
     * @param name The name of the device
     * @param creationDate The creation date of the device
     * @param instanceId The instance id of the device
     * @param deviceInfo The information of the device
     * @param coapClient A CoAP client that can be used to communicate with the device using the IKEA TRÅDFRI gateway
     * @since 1.0.0
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
     * @since 1.0.0
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get the creation date of the device
     * @return The creation date of the device
     * @since 1.0.0
     */
    public Long getCreationDate() {
        return this.creationDate;
    }

    /**
     * Get the instance id of the device
     * @return The instance id of the device
     * @since 1.0.0
     */
    public Integer getInstanceId() {
        return this.instanceId;
    }

    /**
     * Get the information of the device
     * @return The information of the device
     * @since 1.0.0
     */
    public DeviceInfo getDeviceInfo() {
        return this.deviceInfo;
    }

    /**
     * Set the name of the device
     * @param name The name of the device
     * @since 1.0.0
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Set the creation date of the device
     * @param creationDate The creation date of the device
     * @since 1.0.0
     */
    public void setCreationDate(Long creationDate) {
        this.creationDate = creationDate;
    }

    /**
     * Set the instance id of the device
     * @param instanceId The instance id of the device
     * @since 1.0.0
     */
    public void setInstanceId(Integer instanceId) {
        this.instanceId = instanceId;
    }

    /**
     * Get the properties of the device
     * @return The properties of the device
     * @since 1.0.0
     */
    public DeviceProperties getProperties(){
        return this.properties;
    }

    /**
     * Set the properties of the device
     * @param properties The properties of the device
     * @since 1.0.0
     */
    public void setProperties(DeviceProperties properties){
        this.properties = properties;
    }

    /**
     * Get the CoAP endpoint of the device
     * @return The CoAP endpoint of the device
     * @since 1.0.0
     */
    public String getEndpoint(){
        return ApiEndpoint.getUri(ApiEndpoint.DEVICES, String.valueOf(getInstanceId()));
    }

    /**
     * Enable observe to automagically detect changes to the device
     * @return True if successfully enabled observe, false if not
     * @since 1.0.0
     */
    public boolean enableObserve() {
        if(observer == null) observer = new DeviceObserver(this, this.coapClient);
        return observer.start();
    }

    /**
     * Disable observe
     * @return True if successfully disabled observe, false if not
     * @since 1.0.0
     */
    public boolean disableObserve() {
        if(observer == null) return false;
        return observer.stop();
    }

    /**
     * Get a list of event handlers for the device
     * @return A list of event handlers for the device
     * @since 1.0.0
     */
    public List<EventHandler> getEventHandlers(){
        return eventHandlers;
    }

    /**
     * Add an event handler to the device
     * @param eventHandler The event handler to add to the device
     *                     @since 1.0.0
     */
    public void addEventHandler(EventHandler eventHandler){
        this.eventHandlers.add(eventHandler);
    }

    /**
     * Remove an event handler from the device
     * @param eventHandler The event handler to remove from the device
     * @since 1.0.0
     */
    public void removeEventHandler(EventHandler eventHandler){
        this.eventHandlers.remove(eventHandler);
    }

    /**
     * Get the type of the device
     * @return The type of the device
     * @since 1.1.0
     */
    public DeviceType getType(){
        if(isLight()) return DeviceType.LIGHT;
        if(isPlug()) return DeviceType.PLUG;
        if(isRemote()) return DeviceType.REMOTE;
        if(isMotionSensor()) return DeviceType.MOTION_SENSOR;
        return DeviceType.UNKNOWN;
    }

    /**
     * Check if the device is a {@link Light}
     * @return True if the device is a {@link Light}, false if not
     * @since 1.0.0
     */
    public boolean isLight(){
        return this instanceof Light;
    }

    /**
     * Convert the device to the {@link Light} class
     * @return The device as {@link Light}
     * @since 1.0.0
     */
    public Light toLight(){
        if(isLight()) return (Light) this;
        return null;
    }

    /**
     * Check if the device is a {@link Plug}
     * @return True if the device is a {@link Plug}, false if not
     * @since 1.0.0
     */
    public boolean isPlug(){
        return this instanceof Plug;
    }

    /**
     * Convert the device to the {@link Plug} class
     * @return The device as {@link Plug}
     * @since 1.0.0
     */
    public Plug toPlug(){
        if(isPlug()) return (Plug) this;
        return null;
    }

    /**
     * Check if the device is a {@link Remote}
     * @return True if the device is a {@link Remote}, false if not
     * @since 1.0.0
     */
    public boolean isRemote(){
        return this instanceof Remote;
    }

    /**
     * Convert the device to the {@link Remote} class
     * @return The device as {@link Remote}
     * @since 1.0.0
     */
    public Remote toRemote(){
        if(isRemote()) return (Remote) this;
        return null;
    }

    /**
     * Check if the device is a {@link MotionSensor}
     * @return True if the device is a {@link MotionSensor}, false if not
     * @since 1.0.0
     */
    public boolean isMotionSensor(){
        return this instanceof MotionSensor;
    }

    /**
     * Convert the device to the {@link MotionSensor} class
     * @return The device as {@link MotionSensor}
     * @since 1.0.0
     */
    public MotionSensor toMotionSensor(){
        if(isMotionSensor()) return (MotionSensor) this;
        return null;
    }

}
