package org.openremote.agent.protocol.tradfri.device;

import org.openremote.agent.protocol.tradfri.device.Device;
import org.openremote.agent.protocol.tradfri.device.DeviceInfo;
import org.openremote.agent.protocol.tradfri.device.DeviceProperties;
import org.openremote.agent.protocol.tradfri.device.LightProperties;
import org.openremote.agent.protocol.tradfri.payload.DeviceRequest;
import org.openremote.agent.protocol.tradfri.util.ApiEndpoint;
import org.openremote.agent.protocol.tradfri.util.CoapClient;

/**
 * The class that represents an IKEA TRÅDFRI light
 * @author Stijn Groenen
 * @version 1.0.0
 */
public class Light extends Device {

    /**
     * The current properties of the light
     */
    private LightProperties properties;

    /**
     * The new properties of the light in the update queue
     */
    private LightProperties newProperties;

    /**
     * Construct the Light class
     * @param name The name of the light
     * @param creationDate The creation date of the light
     * @param instanceId The instance id of the light
     * @param deviceInfo The information of the device
     * @param properties The properties of the light
     * @param coapClient A CoAP client that can be used to communicate with the light using the IKEA TRÅDFRI gateway
     * @since 1.0.0
     */
    public Light(String name, Long creationDate, Integer instanceId, DeviceInfo deviceInfo, LightProperties properties, CoapClient coapClient){
        super(name, creationDate, instanceId, deviceInfo, coapClient);
        this.properties = properties;
        this.newProperties = new LightProperties();
    }

    /**
     * Get the properties of the light
     * @return The properties of the light
     * @since 1.0.0
     */
    @Override
    public LightProperties getProperties(){
        return this.properties;
    }

    /**
     * Set the properties of the light
     * @param properties The properties of the light
     * @since 1.0.0
     */
    @Override
    public void setProperties(DeviceProperties properties){
        if(properties instanceof LightProperties) this.properties = (LightProperties) properties;
    }

    /**
     * Get the on / off state of the light
     * @return The on / off state of the light (true for on, false for off)
     * @since 1.0.0
     */
    public Boolean getOn() {
        return properties.getOn();
    }

    /**
     * Get the brightness of the light
     * @return The brightness of the light
     * @since 1.0.0
     */
    public Integer getBrightness() {
        return properties.getBrightness();
    }

    /**
     * Get the colour of the light in hexadecimals
     * @return The colour of the light in hexadecimals
     * @since 1.0.0
     */
    public String getColourHex() {
        return properties.getColourHex();
    }

    /**
     * Get the hue of the light
     * @return The hue of the light
     * @since 1.0.0
     */
    public Integer getHue() {
        return properties.getHue();
    }

    /**
     * Get the saturation of the light
     * @return The saturation of the light
     * @since 1.0.0
     */
    public Integer getSaturation() {
        return properties.getSaturation();
    }

    /**
     * Get the X value of the colour of the light
     * @return The X value of the colour of the light
     * @since 1.0.0
     */
    public Integer getColourX() {
        return properties.getColourX();
    }

    /**
     * Get the Y value of the colour of the light
     * @return The Y value of the colour of the light
     * @since 1.0.0
     */
    public Integer getColourY() {
        return properties.getColourY();
    }

    /**
     * Get the colour temperature of the light
     * @return The colour temperature of the light
     * @since 1.0.0
     */
    public Integer getColourTemperature() {
        return properties.getColourTemperature();
    }

    /**
     * Update the on / off state of the light in the update queue
     * @param on The new on / off state for the light (true for on, false for off)
     * @since 1.0.0
     */
    public void updateOn(Boolean on) {
        newProperties.setOn(on);
    }

    /**
     * Update the brightness of the light in the update queue
     * @param brightness The new brightness for the light
     * @since 1.0.0
     */
    public void updateBrightness(Integer brightness) {
        newProperties.setBrightness(brightness);
    }

    /**
     * Update the colour of the light in the update queue to a predefined hexadecimal colour<br>
     * Available colours:<br>
     * <ul>
     *     <li>RGB: {@link org.openremote.agent.protocol.tradfri.util.ColourHex}</li>
     *     <li>Colour temperatures: {@link org.openremote.agent.protocol.tradfri.util.ColourTemperatureHex}</li>
     * </ul>
     * @param colourHex The new colour for the light
     * @since 1.0.0
     */
    public void updateColourHex(String colourHex) {
        newProperties.setColourHex(colourHex);
        newProperties.setHue(null);
        newProperties.setSaturation(null);
        newProperties.setColourX(null);
        newProperties.setColourY(null);
        newProperties.setColourTemperature(null);
    }

    /**
     * Update the hue of the light in the update queue
     * @param hue The new hue for the light
     * @since 1.0.0
     */
    public void updateHue(Integer hue) {
        newProperties.setHue(hue);
        newProperties.setColourX(null);
        newProperties.setColourY(null);
        newProperties.setColourHex(null);
        newProperties.setColourTemperature(null);
    }

    /**
     * Update the saturation of the light in the update queue
     * @param saturation The new saturation for the light
     * @since 1.0.0
     */
    public void updateSaturation(Integer saturation) {
        newProperties.setSaturation(saturation);
        newProperties.setColourX(null);
        newProperties.setColourY(null);
        newProperties.setColourHex(null);
        newProperties.setColourTemperature(null);
    }

    /**
     * Update the colour of the light in the update queue
     * @param colourX The X value of the new colour for the light
     * @param colourY The Y value of the new colour for the light
     * @since 1.0.0
     */
    public void updateColourXY(Integer colourX, Integer colourY) {
        newProperties.setColourX(colourX);
        newProperties.setColourY(colourY);
        newProperties.setColourHex(null);
        newProperties.setHue(null);
        newProperties.setSaturation(null);
        newProperties.setColourTemperature(null);
    }

    /**
     * Update the colour of the light in the update queue
     * @param colourRed The red value of the new colour for the light
     * @param colourGreen The green value of the new colour for the light
     * @param colourBlue The blue value of the new colour for the light
     * @since 1.0.0
     */
    public void updateColourRGB(int colourRed, int colourGreen, int colourBlue) {
        double red = colourRed;
        double green = colourGreen;
        double blue = colourBlue;

        red = (red > 0.04045) ? Math.pow((red + 0.055) / (1.0 + 0.055), 2.4) : (red / 12.92);
        green = (green > 0.04045) ? Math.pow((green + 0.055) / (1.0 + 0.055), 2.4) : (green / 12.92);
        blue = (blue > 0.04045) ? Math.pow((blue + 0.055) / (1.0 + 0.055), 2.4) : (blue / 12.92);

        double X = red * 0.664511 + green * 0.154324 + blue * 0.162028;
        double Y = red * 0.283881 + green * 0.668433 + blue * 0.047685;
        double Z = red * 0.000088 + green * 0.072310 + blue * 0.986039;

        double x = (X / (X + Y + Z));
        double y = (Y / (X + Y + Z));

        int xyX = (int) (x * 65535 + 0.5);
        int xyY = (int) (y * 65535 + 0.5);

        LightProperties newProperties = new LightProperties();
        newProperties.setColourX(xyX);
        newProperties.setColourY(xyY);
        newProperties.setColourHex(null);
        newProperties.setHue(null);
        newProperties.setSaturation(null);
        newProperties.setColourTemperature(null);
    }

    /**
     * Update the colour temperature of the light in the update queue
     * @param colourTemperature The new colour temperature for the light
     * @since 1.0.0
     */
    public void updateColourTemperature(Integer colourTemperature) {
        newProperties.setColourTemperature(colourTemperature);
        newProperties.setColourHex(null);
        newProperties.setHue(null);
        newProperties.setSaturation(null);
        newProperties.setColourX(null);
        newProperties.setColourY(null);
    }

    /**
     * Update the transition time for updating the light in the update queue
     * @param transitionTime The new transition time for updating the light
     * @since 1.0.0
     */
    public void updateTransitionTime(Integer transitionTime){
        newProperties.setTransitionTime(transitionTime);
    }

    /**
     * Set the on / off state of the light
     * @param on The new on / off state for the light (true for on, false for off)
     * @param transitionTime The transition time for updating the light
     * @return True if successfully updated the on / off state of the light, false if not
     * @since 1.0.0
     */
    public boolean setOn(Boolean on, Integer transitionTime) {
        LightProperties newProperties = new LightProperties();
        newProperties.setOn(on);
        newProperties.setTransitionTime(transitionTime);
        return applyUpdate(newProperties);
    }

    /**
     * Set the on / off state of the light
     * @param on The new on / off state for the light (true for on, false for off)
     * @return True if successfully updated the on / off state of the light, false if not
     * @since 1.0.0
     */
    public boolean setOn(Boolean on){
        return setOn(on, null);
    }

    /**
     * Set the brightness of the light
     * @param brightness The new brightness for the light
     * @param transitionTime The transition time for updating the light
     * @return True if successfully updated the brightness of the light, false if not
     * @since 1.0.0
     */
    public boolean setBrightness(Integer brightness, Integer transitionTime) {
        LightProperties newProperties = new LightProperties();
        newProperties.setBrightness(brightness);
        newProperties.setTransitionTime(transitionTime);
        return applyUpdate(newProperties);
    }

    /**
     * Set the brightness of the light
     * @param brightness The new brightness for the light
     * @return True if successfully updated the brightness of the light, false if not
     * @since 1.0.0
     */
    public boolean setBrightness(Integer brightness){
        return setBrightness(brightness, null);
    }

    /**
     * Set the colour of the light to a predefined hexadecimal colour<br>
     * Available colours:<br>
     * <ul>
     *     <li>RGB: {@link org.openremote.agent.protocol.tradfri.util.ColourHex}</li>
     *     <li>Colour temperatures: {@link org.openremote.agent.protocol.tradfri.util.ColourTemperatureHex}</li>
     * </ul>
     * @param colourHex The new colour for the light
     * @param transitionTime The transition time for updating the light
     * @return True if successfully updated the colour of the light, false if not
     * @since 1.0.0
     */
    public boolean setColourHex(String colourHex, Integer transitionTime) {
        LightProperties newProperties = new LightProperties();
        newProperties.setColourHex(colourHex);
        newProperties.setTransitionTime(transitionTime);
        return applyUpdate(newProperties);
    }

    /**
     * Set the colour of the light to a predefined hexadecimal colour<br>
     * Available colours:<br>
     * <ul>
     *     <li>RGB: {@link org.openremote.agent.protocol.tradfri.util.ColourHex}</li>
     *     <li>Colour temperatures: {@link org.openremote.agent.protocol.tradfri.util.ColourTemperatureHex}</li>
     * </ul>
     * @param colourHex The new colour for the light
     * @return True if successfully updated the colour of the light, false if not
     * @since 1.0.0
     */
    public boolean setColourHex(String colourHex){
        return setColourHex(colourHex, null);
    }

    /**
     * Set the hue of the light
     * @param hue The new hue for the light
     * @param transitionTime The transition time for updating the light
     * @return True if successfully updated the hue of the light, false if not
     * @since 1.0.0
     */
    public boolean setHue(Integer hue, Integer transitionTime) {
        LightProperties newProperties = new LightProperties();
        newProperties.setHue(hue);
        newProperties.setTransitionTime(transitionTime);
        return applyUpdate(newProperties);
    }

    /**
     * Set the hue of the light
     * @param hue The new hue for the light
     * @return True if successfully updated the hue of the light, false if not
     * @since 1.0.0
     */
    public boolean setHue(Integer hue){
        return setHue(hue, null);
    }

    /**
     * Set the saturation of the light
     * @param saturation The new saturation for the light
     * @param transitionTime The transition time for updating the light
     * @return True if successfully updated the saturation of the light, false if not
     * @since 1.0.0
     */
    public boolean setSaturation(Integer saturation, Integer transitionTime) {
        LightProperties newProperties = new LightProperties();
        newProperties.setSaturation(saturation);
        newProperties.setTransitionTime(transitionTime);
        return applyUpdate(newProperties);
    }

    /**
     * Set the saturation of the light
     * @param saturation The new saturation for the light
     * @return True if successfully updated the saturation of the light, false if not
     * @since 1.0.0
     */
    public boolean setSaturation(Integer saturation){
        return setSaturation(saturation, null);
    }

    /**
     * Set the colour of the light
     * @param colourX The X value of the new colour for the light
     * @param colourY The Y value of the new colour for the light
     * @param transitionTime The transition time for updating the light
     * @return True if successfully updated the colour of the light, false if not
     * @since 1.0.0
     */
    public boolean setColourXY(Integer colourX, Integer colourY, Integer transitionTime) {
        LightProperties newProperties = new LightProperties();
        newProperties.setColourX(colourX);
        newProperties.setColourY(colourY);
        newProperties.setTransitionTime(transitionTime);
        return applyUpdate(newProperties);
    }

    /**
     * Set the colour of the light
     * @param colourX The X value of the new colour for the light
     * @param colourY The Y value of the new colour for the light
     * @return True if successfully updated the colour of the light, false if not
     * @since 1.0.0
     */
    public boolean setColourXY(Integer colourX, Integer colourY){
        return setColourXY(colourX, colourY, null);
    }

    /**
     * Set the colour of the light
     * @param colourRed The red value of the new colour for the light
     * @param colourGreen The green value of the new colour for the light
     * @param colourBlue The blue value of the new colour for the light
     * @param transitionTime The transition time for updating the light
     * * @return True if successfully updated the colour of the light, false if not
     * @since 1.0.0
     */
    public boolean setColourRGB(int colourRed, int colourGreen, int colourBlue, Integer transitionTime) {
        double red = colourRed;
        double green = colourGreen;
        double blue = colourBlue;

        red = (red > 0.04045) ? Math.pow((red + 0.055) / (1.0 + 0.055), 2.4) : (red / 12.92);
        green = (green > 0.04045) ? Math.pow((green + 0.055) / (1.0 + 0.055), 2.4) : (green / 12.92);
        blue = (blue > 0.04045) ? Math.pow((blue + 0.055) / (1.0 + 0.055), 2.4) : (blue / 12.92);

        double X = red * 0.664511 + green * 0.154324 + blue * 0.162028;
        double Y = red * 0.283881 + green * 0.668433 + blue * 0.047685;
        double Z = red * 0.000088 + green * 0.072310 + blue * 0.986039;

        double x = (X / (X + Y + Z));
        double y = (Y / (X + Y + Z));

        int xyX = (int) (x * 65535 + 0.5);
        int xyY = (int) (y * 65535 + 0.5);

        LightProperties newProperties = new LightProperties();
        newProperties.setColourX(xyX);
        newProperties.setColourY(xyY);
        newProperties.setTransitionTime(transitionTime);
        return applyUpdate(newProperties);
    }

    /**
     * Set the colour of the light
     * @param colourRed The red value of the new colour for the light
     * @param colourGreen The green value of the new colour for the light
     * @param colourBlue The blue value of the new colour for the light
     * @return True if successfully updated the colour of the light, false if not
     * @since 1.0.0
     */
    public boolean setColourRGB(int colourRed, int colourGreen, int colourBlue){
        return setColourRGB(colourRed, colourGreen, colourBlue, null);
    }

    /**
     * Set the colour temperature of the light
     * @param colourTemperature The new colour temperature for the light
     * @param transitionTime The transition time for updating the light
     * @return True if successfully updated the colour temperature of the light, false if not
     * @since 1.0.0
     */
    public boolean setColourTemperature(Integer colourTemperature, Integer transitionTime) {
        LightProperties newProperties = new LightProperties();
        newProperties.setColourTemperature(colourTemperature);
        newProperties.setTransitionTime(transitionTime);
        return applyUpdate(newProperties);
    }

    /**
     * Set the colour temperature of the light
     * @param colourTemperature The new colour temperature for the light
     * @return True if successfully updated the colour temperature of the light, false if not
     * @since 1.0.0
     */
    public boolean setColourTemperature(Integer colourTemperature){
        return setColourTemperature(colourTemperature, null);
    }

    /**
     * Apply updates to the light
     * @param newProperties The new properties to apply to the light
     * @return True if successfully updated the light, false if not
     * @since 1.0.0
     */
    private boolean applyUpdate(LightProperties newProperties) {
        DeviceRequest request = new DeviceRequest();
        request.setLightProperties(new LightProperties[]{newProperties});
        String response = coapClient.put(ApiEndpoint.getUri(ApiEndpoint.DEVICES, String.valueOf(getInstanceId())), request, String.class);
        return response != null;
    }

    /**
     * Apply the updates in the update queue to the light
     * @return True if successfully updated the light, false if not
     * @since 1.0.0
     */
    public boolean applyUpdates() {
        boolean success = applyUpdate(newProperties);
        newProperties = new LightProperties();
        return success;
    }

    /**
     * Apply the updates in the update queue to the light
     * @param transitionTime The transition time for updating the light
     * @return True if successfully updated the light, false if not
     * @since 1.0.0
     */
    public boolean applyUpdates(Integer transitionTime) {
        newProperties.setTransitionTime(transitionTime);
        return applyUpdates();
    }

}
