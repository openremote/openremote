package org.openremote.agent.protocol.tradfri.util;

/**
 * The class that contains helper functions and constants to find CoAP endpoints for the IKEA TRÅDFRI gateway
 */
public class ApiEndpoint {

    /**
     * The IP-address of the IKEA TRÅDFRI gateway
     */
    private static String gatewayIp;

    /**
     * Construct the ApiEndpoint class
     */
    private ApiEndpoint() {
    }

    /**
     * Get the IP-address of the IKEA TRÅDFRI gateway
     * @return The IP-address of the IKEA TRÅDFRI gateway
     */
    public static String getGatewayIp() {
        return ApiEndpoint.gatewayIp;
    }

    /**
     * Set the IP-address of the IKEA TRÅDFRI gateway
     * @param ip The IP-address of the IKEA TRÅDFRI gateway
     */
    public static void setGatewayIp(String ip){
        gatewayIp = ip;
    }

    /**
     * Get the base URL of the IKEA TRÅDFRI API
     * @return The base URL of the IKEA TRÅDFRI API
     */
    public static String getBaseUrl(){
        return "coaps://"+gatewayIp+":5684";
    }

    /**
     * Get the URI of an endpoint of the IKEA TRÅDFRI API
     * @param endpoint The endpoint paths
     * @return The URI of the endpoint of the IKEA TRÅDFRI API
     */
    public static String getUri(String... endpoint){
        return getBaseUrl()+"/"+String.join("/", endpoint);
    }

    // Gateway

    /**
     * The endpoint to authenticate a new client to the IKEA TRÅDFRI gateway<br>
     * <i>Value: {@value}</i>
     */
    public static final String AUTHENTICATE = "15011/9063";

    /**
     * The endpoint to reboot the IKEA TRÅDFRI gateway<br>
     * <i>Value: {@value}</i>
     */
    public static final String GATEWAY_REBOOT = "15011/9030";

    /**
     * The endpoint to reset the IKEA TRÅDFRI gateway<br>
     * <i>Value: {@value}</i>
     */
    public static final String GATEWAY_RESET = "15011/9031";

    /**
     * The endpoint to update the firmware of the IKEA TRÅDFRI gateway<br>
     * <i>Value: {@value}</i>
     */
    public static final String GATEWAY_UPDATE_FIRMWARE = "15011/9034";

    /**
     * The endpoint to get the details of the IKEA TRÅDFRI gateway<br>
     * <i>Value: {@value}</i>
     */
    public static final String GATEWAY_DETAILS = "15011/15012";

    // Global

    /**
     * The endpoint for IKEA TRÅDFRI devices<br>
     * <i>Value: {@value}</i>
     */
    public static final String DEVICES = "15001";

    /**
     * The endpoint for IKEA TRÅDFRI groups<br>
     * <i>Value: {@value}</i>
     */
    public static final String GROUPS = "15004";

    /**
     * The endpoint for IKEA TRÅDFRI scenes<br>
     * <i>Value: {@value}</i>
     */
    public static final String SCENES = "15005";

    /**
     * The endpoint for IKEA TRÅDFRI notifications<br>
     * <i>Value: {@value}</i>
     */
    public static final String NOTIFICATIONS = "15006";

    /**
     * The endpoint for IKEA TRÅDFRI smart tasks<br>
     * <i>Value: {@value}</i>
     */
    public static final String SMART_TASKS = "15010";

}
