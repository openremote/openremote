package org.openremote.agent.protocol.tradfri.util;

import java.awt.*;

/**
 * The class that contains RGB values that make up a colour
 * @author Stijn Groenen
 * @version 1.1.0
 */
public class ColourRGB {

    /**
     * The red value of the colour
     */
    private Integer red;

    /**
     * The green value of the colour
     */
    private Integer green;

    /**
     * The blue value of the colour
     */
    private Integer blue;

    /**
     * Construct the ColourRGB class
     * @param red The red value of the colour
     * @param green The green value of the colour
     * @param blue The blue value of the colour
     * @since 1.1.0
     */
    public ColourRGB(Integer red, Integer green, Integer blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    /**
     * Construct the ColourRGB class
     * @since 1.1.0
     */
    public ColourRGB() {
    }

    /**
     * Get the red value of the colour
     * @return The red value of the colour
     * @since 1.1.0
     */
    public Integer getRed() {
        return this.red;
    }

    /**
     * Get the green value of the colour
     * @return The green value of the colour
     * @since 1.1.0
     */
    public Integer getGreen() {
        return this.green;
    }

    /**
     * Get the blue value of the colour
     * @return The blue value of the colour
     * @since 1.1.0
     */
    public Integer getBlue() {
        return this.blue;
    }

    /**
     * Set the red value of the colour
     * @param red The new red value of the colour
     * @since 1.1.0
     */
    public void setRed(Integer red) {
        this.red = red;
    }

    /**
     * Set the green value of the colour
     * @param green The new green value of the colour
     * @since 1.1.0
     */
    public void setGreen(Integer green) {
        this.green = green;
    }

    /**
     * Set the blue value of the colour
     * @param blue The new blue value of the colour
     * @since 1.1.0
     */
    public void setBlue(Integer blue) {
        this.blue = blue;
    }

    /**
     * Convert to the {@link ColourXY} class
     * @return The {@link ColourXY} class
     * @since 1.1.0
     */
    public ColourXY toXY(){
        double red = Math.max(Math.min(this.red, 255), 0);
        double green = Math.max(Math.min(this.green, 255), 0);
        double blue = Math.max(Math.min(this.blue, 255), 0);

        red = red / 255;
        green = green / 255;
        blue = blue / 255;

        red = (red > 0.04045) ? Math.pow((red + 0.055) / (1.0 + 0.055), 2.4) : (red / 12.92);
        green = (green > 0.04045) ? Math.pow((green + 0.055) / (1.0 + 0.055), 2.4) : (green / 12.92);
        blue = (blue > 0.04045) ? Math.pow((blue + 0.055) / (1.0 + 0.055), 2.4) : (blue / 12.92);

        double X = red * 0.4124564 + green * 0.3575761 + blue * 0.1804375;
        double Y = red * 0.2126729 + green * 0.7151522 + blue * 0.0721750;
        double Z = red * 0.0193339 + green * 0.1191920 + blue * 0.9503041;
        double total = X + Y + Z;

        double x = 0;
        double y = 0;
        if(total != 0){
            x = X / total;
            y = Y / total;
        }

        int xNormalised = (int) (x * 65535 + 0.5);
        int yNormalised = (int) (y * 65535 + 0.5);

        return new ColourXY(xNormalised, yNormalised);
    }

    /**
     * Construct the ColourRGB class from the hue and saturation values
     * @param hue The hue value
     * @param saturation The saturation value
     * @return The ColourRGB class
     * @since 1.1.0
     */
    public static ColourRGB fromHS(int hue, int saturation){
        float hueNormalised = hue/65535f;
        float saturationNormalised = saturation/65535f;
        int rgb = Color.HSBtoRGB(hueNormalised, saturationNormalised, 1);
        Color colour = new Color(rgb);
        return new ColourRGB(colour.getRed(), colour.getGreen(), colour.getBlue());
    }
}
