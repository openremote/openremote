package org.openremote.agent.protocol.tradfri.device;

import org.openremote.agent.protocol.tradfri.util.CoapClient;

/**
 * The class that represents an IKEA TRÅDFRI motion sensor
 * @author Stijn Groenen
 * @version 1.0.0
 */
public class MotionSensor extends Device {

    /**
     * Construct the MotionSensor class
     * @param name The name of the motion sensor
     * @param creationDate The creation date of the motion sensor
     * @param instanceId The instance id of the motion sensor
     * @param deviceInfo The information of the device
     * @param coapClient A CoAP client that can be used to communicate with the plug using the IKEA TRÅDFRI gateway
     * @since 1.0.0
     */
    public MotionSensor(String name, Long creationDate, Integer instanceId, DeviceInfo deviceInfo, CoapClient coapClient){
        super(name, creationDate, instanceId, deviceInfo, coapClient);
    }

}
