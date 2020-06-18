package org.openremote.agent.protocol.tradfri.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openremote.agent.protocol.tradfri.device.LightProperties;
import org.openremote.agent.protocol.tradfri.device.PlugProperties;
import org.openremote.agent.protocol.tradfri.util.ApiCode;

/**
 * The class that contains the payload for a request to update a IKEA TRÅDFRI device
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeviceRequest {

    /**
     * The new properties of the light (if the device is a light)
     */
    @JsonProperty(ApiCode.LIGHT)
    private LightProperties[] lightProperties;

    /**
     * The new properties of the plug (if the device is a plug)
     */
    @JsonProperty(ApiCode.PLUG)
    private PlugProperties[] plugProperties;

    /**
     * Construct the DeviceRequest class
     */
    public DeviceRequest(){
    }

    /**
     * Get the new properties of the light (if the device is a light)
     * @return The new properties of the light
     */
    public LightProperties[] getLightProperties() {
        return this.lightProperties;
    }

    /**
     * Get the new properties of the plug (if the device is a plug)
     * @return The new properties of the plug
     */
    public PlugProperties[] getPlugProperties() {
        return this.plugProperties;
    }

    /**
     * Set the new properties of the light (if the device is a light)
     * @param lightProperties The new properties of the light
     */
    public void setLightProperties(LightProperties[] lightProperties) {
        this.lightProperties = lightProperties;
    }

    /**
     * Set the new properties of the plug (if the device is a plug)
     * @param plugProperties The new properties of the plug
     */
    public void setPlugProperties(PlugProperties[] plugProperties) {
        this.plugProperties = plugProperties;
    }
}
