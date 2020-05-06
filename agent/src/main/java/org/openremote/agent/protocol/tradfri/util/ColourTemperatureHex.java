package org.openremote.agent.protocol.tradfri.util;

/**
 * The class that contains constants for hexadecimal colour temperatures allowed by the IKEA TRÅDFRI API
 * @author Stijn Groenen
 * @version 1.0.0
 */
public class ColourTemperatureHex {

    /**
     * Construct the ColourTemperatureHex class
     * @since 1.0.0
     */
    private ColourTemperatureHex() {
    }

    /**
     * The hexadecimal colour for the colour temperature "white"<br>
     * <i>Value: {@value}</i>
     */
    public static final String WHITE = "f5faf6";

    /**
     * The hexadecimal colour for the colour temperature "warm"<br>
     * <i>Value: {@value}</i>
     */
    public static final String WARM = "f1e0b5";

    /**
     * The hexadecimal colour for the colour temperature "glow"<br>
     * <i>Value: {@value}</i>
     */
    public static final String GLOW = "efd275";

}
