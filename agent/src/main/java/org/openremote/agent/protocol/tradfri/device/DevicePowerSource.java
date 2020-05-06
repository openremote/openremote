package org.openremote.agent.protocol.tradfri.device;

/**
 * The enum that contains the available power sources for an IKEA TRÅDFRI device
 * @author Stijn Groenen
 * @version 1.0.0
 */
public enum DevicePowerSource {
    Unknown,
    InternalBattery,
    ExternalBattery,
    Battery,
    PowerOverEthernet,
    USB,
    AcPower,
    Solar
}
