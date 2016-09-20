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
import org.openremote.entities.controller.Device;
import org.openremote.entities.controller.Sensor;
import org.openremote.manager.shared.asset.Asset;
import org.openremote.manager.shared.asset.AssetType;
import org.openremote.manager.shared.attribute.AttributeType;
import org.openremote.manager.shared.device.DeviceAttributes;
import org.openremote.manager.shared.device.DeviceResource;

import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Stream;

/**
 * Contains the data to allow mapping between native device API and the component device API
 */
public class DeviceMapping {

    protected Device device;
    protected Asset deviceAsset;
    protected DeviceRegistrationHandle registrationHandle;
    protected Map<String, DeviceResourceMapping> resourceMap = new HashMap<>();

    public void addSensorListener(SensorListener listener) {
        for (DeviceResourceMapping deviceResourceMapping : resourceMap.values()) {
            deviceResourceMapping.addSensorListener(listener);
        }
    }

    public void removeSensorListener(SensorListener listener) {
        for (DeviceResourceMapping deviceResourceMapping : resourceMap.values()) {
            deviceResourceMapping.removeSensorListener(listener);
        }
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public Asset getDeviceAsset() {
        return deviceAsset;
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

    public void update(List<Controller.WidgetCommandInfo> widgetCommandInfos) {
        deviceAsset = null;
        Device device = getDevice();

        // Remove sensor change listeners from sensors
        // TODO Not sure this is necessary, as we always have fresh instances (Device, Sensor) during update...
        resourceMap.values().forEach(DeviceResourceMapping::detachSensorListeners);

        if (device != null) {

            // TODO Take first command protocol as the device type?
            // String deviceProtocol = device.getCommands().get(0).getProtocol().toUpperCase();

            deviceAsset = new Asset(device.getName(), AssetType.DEVICE);

            // TODO: Stable device identifier generation?
            deviceAsset.setId(
                IdentifierUtil.getEncodedHash(
                    device.getName().toLowerCase(Locale.ROOT).getBytes(Charset.forName("utf-8"))
                )
            );

            DeviceAttributes deviceAttributes = new DeviceAttributes(deviceAsset.getAttributes());
            deviceAttributes.setKey(device.getName().toLowerCase(Locale.ROOT));

            if (device.getCommands() == null || device.getCommands().size() == 0) {
                return;
            }

            // TODO: Controller API needs to provide Make & model info for the device
            //deviceAttributes.setMakeAndModel();


            // Get Device resources and capabilities
            List<Integer> assignedCommands = new ArrayList<>();

            // Go through sensors - any used by slider and switch
            // widgets can be RW resources
            for (Sensor sensor : device.getSensors()) {
                Command sensorCommand = sensor.getCommand();
                assignedCommands.add(sensorCommand.getId());
                Controller.WidgetCommandInfo matchingWidgetInfo =
                    widgetCommandInfos.stream()
                        .filter(widgetCommandInfo -> widgetCommandInfo.getSensorId() == sensor.getId())
                        .findFirst()
                        .orElseGet(() -> null);

                AttributeType resourceType;
                DeviceResource.Access access;
                switch (sensor.getType()) {
                    case SWITCH:
                        resourceType = AttributeType.BOOLEAN;
                        access = matchingWidgetInfo != null ? DeviceResource.Access.RW : DeviceResource.Access.R;
                        break;
                    case RANGE:
                    case LEVEL:
                        resourceType = AttributeType.INTEGER;
                        access = matchingWidgetInfo != null ? DeviceResource.Access.RW : DeviceResource.Access.R;
                        break;
                    default:
                        resourceType = AttributeType.STRING;
                        access = DeviceResource.Access.R;
                        break;
                }

                String deviceResourceKey = sensor.getName().toLowerCase(Locale.ROOT);

                DeviceResource deviceResource = new DeviceResource(
                    sensor.getName(),
                    deviceResourceKey,
                    resourceType,
                    access
                );

                DeviceResourceMapping resourceMapping = new DeviceResourceMapping(deviceResourceKey);
                resourceMapping.setResource(deviceResource);
                resourceMapping.setSensor(sensor);
                resourceMap.put(deviceResource.getValueAsString(), resourceMapping);

                deviceAttributes.put(deviceResource);

                if (matchingWidgetInfo != null) {
                    if (matchingWidgetInfo.getCommandId1() > 0) {
                        assignedCommands.add(matchingWidgetInfo.getCommandId1());
                        resourceMapping.setSendCommand1(
                            device.getCommands().stream()
                                .filter(command -> command.getId() == matchingWidgetInfo.getCommandId1())
                                .findFirst()
                                .orElseGet(() -> null)
                        );
                    }
                    if (matchingWidgetInfo.getCommandId2() > 0) {
                        resourceMapping.setSendCommand2(
                            device.getCommands().stream()
                                .filter(command -> command.getId() == matchingWidgetInfo.getCommandId2())
                                .findFirst()
                                .orElseGet(() -> null)
                        );
                        assignedCommands.add(matchingWidgetInfo.getCommandId2());
                    }
                }
            }

            // Add any commands not already covered as write resources
            // Assume they are W resources with null data type
            Stream<Command> unassignedCommands = device.getCommands().stream().filter(
                command -> !assignedCommands.contains(command.getId())
            );

            // TODO: All commands are of string type?
            unassignedCommands.forEach(command -> {

                String deviceResourceKey = command.getName().toLowerCase(Locale.ROOT);

                DeviceResource deviceResource = new DeviceResource(
                    command.getName(),
                    deviceResourceKey,
                    AttributeType.STRING,
                    DeviceResource.Access.W
                );

                DeviceResourceMapping resourceMapping = new DeviceResourceMapping(deviceResourceKey);
                resourceMapping.setResource(deviceResource);
                resourceMapping.setSendCommand1(command);
                getResourceMap().put(deviceResource.getValueAsString(), resourceMapping);

                deviceAttributes.put(deviceResource);
            });

            deviceAsset.setAttributes(deviceAttributes.getJsonObject());
        }
    }
}
