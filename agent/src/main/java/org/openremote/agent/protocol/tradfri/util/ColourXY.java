package org.openremote.agent.protocol.tradfri.util;

/**
 * The class that contains XY values that make up a colour
 */
public class ColourXY {

    /**
     * The X value of the colour
     */
    private Integer X;

    /**
     * The Y value of the colour
     */
    private Integer Y;

    /**
     * Construct the ColourXY class
     * @param X The X value of the colour
     * @param Y The Y value of the colour
     */
    public ColourXY(Integer X, Integer Y) {
        this.X = X;
        this.Y = Y;
    }

    /**
     * Construct the ColourXY class
     */
    public ColourXY() {
    }

    /**
     * Get the X value of the colour
     * @return The X value of the colour
     */
    public Integer getX() {
        return this.X;
    }

    /**
     * Get the Y value of the colour
     * @return The Y value of the colour
     */
    public Integer getY() {
        return this.Y;
    }

    /**
     * Set the X value of the colour
     * @param X The new X value of the colour
     */
    public void setX(Integer X) {
        this.X = X;
    }

    /**
     * Set the Y value of the colour
     * @param Y The new Y value of the colour
     */
    public void setY(Integer Y) {
        this.Y = Y;
    }

    /**
     * Construct the ColourXY class from the {@link ColourRGB} class
     * @param colourRGB The {@link ColourRGB} class
     * @return The ColourXY class
     */
    public static ColourXY fromRGB(ColourRGB colourRGB){
        return colourRGB.toXY();
    }
}
