package org.openremote.agent.protocol.tradfri.device.event;

import org.openremote.agent.protocol.tradfri.device.MotionSensor;

/**
 * The class that represents a motion sensor event that occurred to an IKEA TRÅDFRI motion sensor
 */
public class MotionSensorEvent extends DeviceEvent {

    /**
     * Construct the MotionSensorEvent class
     * @param motionSensor The motion sensor for which the event occurred
     */
    public MotionSensorEvent(MotionSensor motionSensor) {
        super(motionSensor);
    }

    /**
     * Get the motion sensor for which the event occurred
     * @return The motion sensor for which the event occurred
     */
    public MotionSensor getMotionSensor(){
        return (MotionSensor) getDevice();
    }

}
