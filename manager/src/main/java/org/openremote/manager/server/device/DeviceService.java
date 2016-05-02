package org.openremote.manager.server.device;

import org.openremote.manager.shared.device.Device;
import org.openremote.manager.shared.gateway.Gateway;

import java.util.List;

public interface DeviceService {
    List<Gateway> getGateways();

    void addGateway(Gateway gateway);

    void removeGateway(Gateway gateway);

    void stopGateway(Gateway gateway);

    void startGateway(Gateway gateway);

    int getGatewayId(Gateway gateway);

    List<Device> getDevices();

    void addDevice(int gatewayId, Device device);

    void removeDevice(Device device);
}
