package org.openremote.agent.protocol.tradfri.device;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openremote.agent.protocol.tradfri.util.ApiCode;

/**
 * The class that contains the information of an IKEA TRÅDFRI device
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeviceInfo extends DeviceProperties{

    /**
     * The manufacturer of the device
     */
    @JsonProperty(ApiCode.MANUFACTURER)
    private String manufacturer;

    /**
     * The model name of the device
     */
    @JsonProperty(ApiCode.MODEL_NAME)
    private String modelName;

    /**
     * The serial of the device
     */
    @JsonProperty(ApiCode.SERIAL)
    private String serial;

    /**
     * The firmware version of the device
     */
    @JsonProperty(ApiCode.FIRMWARE_VERSION)
    private String firmwareVersion;

    /**
     * The battery level of the device
     */
    @JsonProperty(ApiCode.BATTERY_LEVEL)
    private Integer batteryLevel;

    /**
     * Construct the DeviceInfo class
     */
    public DeviceInfo(){
    }

    /**
     * Get the manufacturer of the device
     * @return The manufacturer of the device
     */
    public String getManufacturer() {
        return this.manufacturer;
    }

    /**
     * Get the model name of the device
     * @return The model name of the device
     */
    public String getModelName() {
        return this.modelName;
    }

    /**
     * Get the serial of the device
     * @return The serial of the device
     */
    public String getSerial() {
        return this.serial;
    }

    /**
     * Get the firmware version of the device
     * @return The firmware version of the device
     */
    public String getFirmwareVersion() {
        return this.firmwareVersion;
    }

    /**
     * Get the battery level of the device
     * @return The battery level of the device
     */
    public Integer getBatteryLevel() {
        return this.batteryLevel;
    }

    /**
     * Set the manufacturer of the device<br>
     * <i>Note: This does not change the actual device</i>
     * @param manufacturer The new manufacturer of the device
     */
    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    /**
     * Set the model name of the device<br>
     * <i>Note: This does not change the actual device</i>
     * @param modelName The new model name of the device
     */
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    /**
     * Set the serial of the device<br>
     * <i>Note: This does not change the actual device</i>
     * @param serial The new serial of the device
     */
    public void setSerial(String serial) {
        this.serial = serial;
    }

    /**
     * Set the firmware version of the device<br>
     * <i>Note: This does not change the actual device</i>
     * @param firmwareVersion The new firmware version of the device
     */
    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    /**
     * Set the battery level of the device<br>
     * <i>Note: This does not change the actual device</i>
     * @param batteryLevel The new battery level of the device
     */
    public void setBatteryLevel(Integer batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

}
