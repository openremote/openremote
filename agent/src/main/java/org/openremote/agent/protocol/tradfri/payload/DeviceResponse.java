package org.openremote.agent.protocol.tradfri.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openremote.agent.protocol.tradfri.device.DeviceInfo;
import org.openremote.agent.protocol.tradfri.device.LightProperties;
import org.openremote.agent.protocol.tradfri.device.PlugProperties;
import org.openremote.agent.protocol.tradfri.util.ApiCode;

/**
 * The class that contains the payload for a response of an IKEA TRÅDFRI device
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceResponse {

    /**
     * The name of the device
     */
    @JsonProperty(ApiCode.NAME)
    private String name;

    /**
     * The creation date of the device
     */
    @JsonProperty(ApiCode.CREATION_DATE)
    private Long creationDate;

    /**
     * The instance id of the device
     */
    @JsonProperty(ApiCode.INSTANCE_ID)
    private Integer instanceId;

    /**
     * The information of the device
     */
    @JsonProperty(ApiCode.DEVICE_INFORMATION)
    private DeviceInfo deviceInfo;

    /**
     * The properties of the light (if the device is a light)
     */
    @JsonProperty(ApiCode.LIGHT)
    private LightProperties[] lightProperties;

    /**
     * The properties of the plug (if the device is a plug)
     */
    @JsonProperty(ApiCode.PLUG)
    private PlugProperties[] plugProperties;

    /**
     * Construct the DeviceResponse class
     */
    public DeviceResponse(){
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
     * @return The instance id of the device
     */
    public DeviceInfo getDeviceInfo() {
        return this.deviceInfo;
    }

    /**
     * Get the properties of the light (if the device is a light)
     * @return The properties of the light
     */
    public LightProperties[] getLightProperties() {
        return this.lightProperties;
    }

    /**
     * Get the properties of the plug (if the device is a plug)
     * @return The properties of the plug
     */
    public PlugProperties[] getPlugProperties() {
        return this.plugProperties;
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
     * Set the information of the device
     * @param deviceInfo The information of the device
     */
    public void setDeviceInfo(DeviceInfo deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    /**
     * Set the properties of the light (if the device is a light)
     * @param lightProperties The new properties of the light
     */
    public void setLightProperties(LightProperties[] lightProperties) {
        this.lightProperties = lightProperties;
    }

    /**
     * Set the properties of the plug (if the device is a plug)
     * @param plugProperties The new properties of the plug
     */
    public void setPlugProperties(PlugProperties[] plugProperties) {
        this.plugProperties = plugProperties;
    }
}
