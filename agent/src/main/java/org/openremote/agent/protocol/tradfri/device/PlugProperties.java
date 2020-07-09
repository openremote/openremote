package org.openremote.agent.protocol.tradfri.device;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openremote.agent.protocol.tradfri.util.ApiCode;

/**
 * The class that contains the properties of an IKEA TRÅDFRI plug
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlugProperties extends DeviceProperties {

    /**
     * The on state of the plug (true for on, false for off)
     */
    @JsonProperty(ApiCode.ON_OFF)
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Boolean on;

    /**
     * Construct the PlugProperties class
     */
    public PlugProperties(){
    }

    /**
     * Get the on state of the plug
     * @return The on state of the plug (true for on, false for off)
     */
    public Boolean getOn() {
        return this.on;
    }

    /**
     * Set the on state of the plug within the PlugProperties class<br>
     * <i>Note: This does not change the actual plug</i>
     * @param on The new on state for the plug (true for on, false for off)
     */
    public void setOn(Boolean on) {
        this.on = on;
    }
}
