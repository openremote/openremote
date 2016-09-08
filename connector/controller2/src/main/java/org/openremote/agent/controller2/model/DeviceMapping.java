/*
 * Copyright 2016, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.agent.controller2.model;

import org.openremote.console.controller.Controller;
import org.openremote.console.controller.DeviceRegistrationHandle;
import org.openremote.container.util.IdentifierUtil;
import org.openremote.entities.controller.Command;
import org.openremote.entities.controller.Sensor;
import org.openremote.manager.shared.attribute.AttributeType;
import org.openremote.manager.shared.device.Device;
import org.openremote.manager.shared.device.DeviceResource;

import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Stream;

/**
 * Contains the data to allow mapping between native device API and the component device API
 */
public class DeviceMapping {

    protected org.openremote.entities.controller.Device gatewayDevice;
    protected Device device;
    protected DeviceRegistrationHandle registrationHandle;
    protected Map<String, DeviceResourceMapping> resourceMap = new HashMap<>();

    public org.openremote.entities.controller.Device getGatewayDevice() {
        return gatewayDevice;
    }

    public void setGatewayDevice(org.openremote.entities.controller.Device gatewayDevice) {
        this.gatewayDevice = gatewayDevice;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public DeviceRegistrationHandle getRegistrationHandle() {
        return registrationHandle;
    }

    public void setRegistrationHandle(DeviceRegistrationHandle registrationHandle) {
        this.registrationHandle = registrationHandle;
    }

    public Map<String, DeviceResourceMapping> getResourceMap() {
        return resourceMap;
    }

    public void setResourceMap(Map<String, DeviceResourceMapping> resourceMap) {
        this.resourceMap = resourceMap;
    }

    public void update(List<Controller.WidgetCommandInfo> widgetCommandInfos) {
        setDevice(null);
        org.openremote.entities.controller.Device gatewayDevice = getGatewayDevice();

        if (gatewayDevice != null) {
            Device device = new Device();
            setDevice(device);
            // TODO: Stable device identifier generation?
            device.setId(
                IdentifierUtil.getEncodedHash(
                    gatewayDevice.getName().toLowerCase(Locale.ROOT).getBytes(Charset.forName("utf-8"))
                )
            );
            device.setUri(gatewayDevice.getName());
            device.setName(gatewayDevice.getName());

            if (gatewayDevice.getCommands() == null || gatewayDevice.getCommands().size() == 0) {
                return;
            }

            // Get Device type
            // TODO: Controller API needs to provide Make & model info for the device
            // Take first command protocol as the device type
            device.setType(gatewayDevice.getCommands().get(0).getProtocol().toUpperCase());

            // Get Device resources and capabilities
            List<DeviceResource> resources = new ArrayList<>();
            List<Integer> assignedCommands = new ArrayList<>();

            // Go through sensors - any used by slider and switch
            // widgets can be RW resources
            for (Sensor sensor : gatewayDevice.getSensors()) {
                Command sensorCommand = sensor.getCommand();
                assignedCommands.add(sensorCommand.getId());
                Controller.WidgetCommandInfo matchingWidgetInfo =
                    widgetCommandInfos.stream()
                        .filter(widgetCommandInfo -> widgetCommandInfo.getSensorId() == sensor.getId())
                        .findFirst()
                        .orElseGet(() -> null);

                DeviceResource dr = new DeviceResource(sensor.getName());
                dr.setUri(sensor.getName().toLowerCase(Locale.ROOT));
                DeviceResourceMapping resourceMapping = new DeviceResourceMapping();
                resourceMapping.setResource(dr);
                resourceMapping.setGatewaySensor(sensor);
                resources.add(dr);
                getResourceMap().put(dr.getUri(), resourceMapping);

                if (matchingWidgetInfo != null) {
                    if (matchingWidgetInfo.getCommandId1() > 0) {
                        assignedCommands.add(matchingWidgetInfo.getCommandId1());
                        resourceMapping.setSendCommand1(
                            gatewayDevice.getCommands().stream()
                                .filter(command -> command.getId() == matchingWidgetInfo.getCommandId1())
                                .findFirst()
                                .orElseGet(() -> null)
                        );
                    }
                    if (matchingWidgetInfo.getCommandId2() > 0) {
                        resourceMapping.setSendCommand2(
                            gatewayDevice.getCommands().stream()
                                .filter(command -> command.getId() == matchingWidgetInfo.getCommandId2())
                                .findFirst()
                                .orElseGet(() -> null)
                        );
                        assignedCommands.add(matchingWidgetInfo.getCommandId2());
                    }
                }

                switch (sensor.getType()) {
                    case SWITCH:
                        dr.setType(AttributeType.BOOLEAN);
                        dr.setAccess(matchingWidgetInfo != null ? DeviceResource.Access.RW : DeviceResource.Access.R);
                        break;
                    case RANGE:
                    case LEVEL:
                        dr.setType(AttributeType.INTEGER);
                        dr.setAccess(matchingWidgetInfo != null ? DeviceResource.Access.RW : DeviceResource.Access.R);
                        break;
                    default:
                        dr.setType(AttributeType.STRING);
                        dr.setAccess(DeviceResource.Access.R);
                        break;
                }
            }

            // Add any commands not already covered as write resources
            // Assume they are W resources with null data type
            Stream<Command> unassignedCommands = gatewayDevice.getCommands().stream().filter(command -> !assignedCommands.contains(command.getId()));

            unassignedCommands.forEach(command -> {
                DeviceResource dr = new DeviceResource(command.getName());
                dr.setUri(command.getName().toLowerCase(Locale.ROOT));
                dr.setType(null);
                dr.setAccess(DeviceResource.Access.W);

                DeviceResourceMapping resourceMapping = new DeviceResourceMapping();
                resourceMapping.setResource(dr);
                resourceMapping.setSendCommand1(command);
                resources.add(dr);
                getResourceMap().put(dr.getUri(), resourceMapping);
            });

            device.setResources(resources.toArray(new DeviceResource[resources.size()]));
        }
    }
}
