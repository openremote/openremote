package org.openremote.agent.protocol.tradfri.device;

/**
 * The enum that contains the available power sources for an IKEA TRÅDFRI device
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
