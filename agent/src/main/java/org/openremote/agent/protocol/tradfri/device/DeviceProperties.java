package org.openremote.agent.protocol.tradfri.device;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openremote.agent.protocol.tradfri.util.ApiCode;

/**
 * The class that contains the properties of an IKEA TRÅDFRI device
 * @author Stijn Groenen
 * @version 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeviceProperties {

    /**
     * The instance id of the device
     */
    @JsonProperty(ApiCode.INSTANCE_ID)
    private Integer instanceId;

    /**
     * Construct the DeviceProperties class
     * @since 1.0.0
     */
    public DeviceProperties(){
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
     * Set the instance id of the device
     * @param instanceId The instance id of the device
     * @since 1.0.0
     */
    public void setInstanceId(Integer instanceId) {
        this.instanceId = instanceId;
    }
}
