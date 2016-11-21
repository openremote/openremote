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

import org.openremote.console.controller.AsyncRegistrationCallback;
import org.openremote.console.controller.Controller;
import org.openremote.entities.controller.*;
import org.openremote.manager.shared.Consumer;
import org.openremote.manager.shared.device.DeviceAttributes;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ControllerState {

    private static final Logger LOG = Logger.getLogger(ControllerState.class.getName());

    final protected String controllerUrl;

    protected volatile boolean initialized = false;
    // TODO: The concurrency control of these data structures should be revisited when we deal with reconnect/refresh of devices
    final protected Map<String, DeviceMapping> deviceMap = new LinkedHashMap<>();
    final protected List<SensorListener> sensorListeners = new CopyOnWriteArrayList<>();
    final protected List<DeviceListener> deviceListeners = new CopyOnWriteArrayList<>();
    final protected List<DeviceListener> queuedDeviceListeners = new CopyOnWriteArrayList<>();
    final protected List<WriteResourceRequest> queuedWriteRequests = new CopyOnWriteArrayList<>();

    public ControllerState(String controllerUrl) {
        this.controllerUrl = controllerUrl;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public void initialize(Controller controller) {
        LOG.fine("Initializing controller state: " + controllerUrl);
        // TODO: Update the controller library to return futures
        // TODO: Resync the devices after disconnect/reconnect
        if (initialized)
            return;

        try {
            deviceMap.clear();
            updateDeviceMappings(controller, deviceMapping -> {
                String deviceKey = new DeviceAttributes(deviceMapping.getDeviceAsset().getAttributes()).getKey();
                LOG.fine("Updating device mapping: " + deviceKey);
                deviceMap.put(deviceKey, deviceMapping);
            });
            initialized = true;
            LOG.fine("Controller state initialized");
        } catch (InterruptedException e) {
            initialized = false;
            LOG.severe("Controller state initialization was interrupted");
        } catch (Exception ex) {
            initialized = false;
            // TODO: better error handling
            LOG.log(Level.SEVERE, "Controller state initialization failed", ex);
        }

        LOG.fine("Processing sensor listeners: " + sensorListeners.size());
        sensorListeners.forEach(this::processSensorListener);

        LOG.fine("Processing queued device listeners: " + queuedDeviceListeners.size());
        queuedDeviceListeners.stream().forEach(this::announceDevices);
        queuedDeviceListeners.clear();

        LOG.fine("Processing queued write requests: " + queuedWriteRequests.size());
        queuedWriteRequests.stream().forEach(this::processWriteRequest);
        queuedWriteRequests.clear();

    }

    public void addSensorListener(SensorListener listener) {
        if (listener != null && (listener.getDeviceKey() == null)) {
            LOG.warning("Ignoring invalid sensor listener (" + controllerUrl + "), missing device key: " + listener);
            return;
        }
        LOG.fine("Adding sensor listener (" + controllerUrl + "): " + listener);
        sensorListeners.add(listener);
        processSensorListener(listener);
    }

    public void removeSensorListener(SensorListener listener) {
        LOG.fine("Removing sensor listener (" + controllerUrl + "): " + listener);
        sensorListeners.remove(listener);
        if (listener.getDeviceKey() == null) {
            return;
        }
        // Unregister the handler
        DeviceMapping deviceMapping = deviceMap.get(listener.getDeviceKey());

        if (deviceMapping == null) {
            LOG.fine("Device key for sensor listener cannot be found so ignoring: " + listener.getDeviceKey());
            return;
        }

        deviceMapping.removeSensorListener(listener);
    }

    public void addDeviceListener(DeviceListener listener) {
        LOG.fine("Adding device listener (" + controllerUrl + "): " + listener);
        deviceListeners.add(listener);

        if (!initialized) {
            queuedDeviceListeners.add(listener);
            return;
        }
        announceDevices(listener);
    }

    public void removeDeviceListener(DeviceListener listener) {
        LOG.fine("Removing device listener (" + controllerUrl + "): " + listener);
        deviceListeners.remove(listener);
    }


    public void triggerDiscovery() {
        LOG.fine("Triggering discovery (" + controllerUrl + ")");
        deviceListeners.forEach(this::announceDevices);
    }

    public Object readResource(String deviceKey, String resourceKey) {
        LOG.fine("Reading resource (" + controllerUrl + "): " + deviceKey + "/" + resourceKey);

        DeviceMapping deviceMapping = deviceMap.get(deviceKey);
        if (deviceMapping == null) {
            LOG.info("Request to read to unknown device key '" + deviceKey + "' will be ignored");
            return null;
        }

        DeviceResourceMapping resourceMapping = deviceMapping.getResourceMap().get(resourceKey);
        if (resourceMapping == null) {
            LOG.info("Request to read to unknown resource key '" + resourceKey + "' will be ignored");
            return null;
        }

        if (resourceMapping.getSensor() == null) {
            LOG.info("Request to read non-sensor device resource '" + resourceKey + "' will be ignored");
            return null;
        }

        // TODO: This only reads the cached value?! It doesn't actually call a command on the controller...
        return resourceMapping.getSensor().getStringValue();
    }

    public void writeResource(String deviceKey, String resourceKey, Object resourceValue) {
        LOG.fine("Writing resource (" + controllerUrl + "): " + deviceKey + "/" + resourceKey + ": " + resourceValue);

        if (!initialized) {
            LOG.fine("Controller state not initialized, queuing write request");
            WriteResourceRequest writeRequest = new WriteResourceRequest();
            writeRequest.setDeviceKey(deviceKey);
            writeRequest.setResourceKey(resourceKey);
            writeRequest.setResourceValue(resourceValue);
            queuedWriteRequests.add(writeRequest);
            return;
        }

        processWriteRequest(new WriteResourceRequest(deviceKey, resourceKey, resourceValue));
    }

    /* ################################################################################## */

    protected void updateDeviceMappings(Controller controller, Consumer<DeviceMapping> onDeviceMappingUpdate) throws InterruptedException {
        List<String> deviceNames = new ArrayList<>();
        List<Controller.WidgetCommandInfo> widgetCommandInfos = new ArrayList<>();

        // All of the following blocks must be executed serially
        {
            final CountDownLatch latch = new CountDownLatch(1);
            // Get widget command information to allow capability
            // construction and writable resource resolution
            controller.getWidgetsCommandsInfo(new AsyncControllerCallback<List<Controller.WidgetCommandInfo>>() {
                @Override
                public void onFailure(ControllerResponseCode controllerResponseCode) {
                    LOG.severe("Failed to get widget command info from the controller: " + controllerResponseCode);
                    latch.countDown();
                }

                @Override
                public void onSuccess(List<Controller.WidgetCommandInfo> result) {
                    LOG.fine("Got widget command info from controller");
                    widgetCommandInfos.addAll(result);
                    latch.countDown();
                }
            });
            latch.await();
        }

        {
            final CountDownLatch latch = new CountDownLatch(1);
            // Get device names available on this controller
            controller.getDeviceList(new AsyncControllerCallback<List<DeviceInfo>>() {
                @Override
                public void onFailure(ControllerResponseCode controllerResponseCode) {
                    LOG.severe("Failed to get device list from the controller: " + controllerResponseCode);
                    latch.countDown();
                }

                @Override
                public void onSuccess(List<DeviceInfo> deviceInfos) {
                    LOG.fine("Got device list from controller");
                    deviceNames.addAll(deviceInfos.stream().map(DeviceInfo::getName).collect(Collectors.toList()));
                    latch.countDown();
                }
            });
            latch.await();
        }

        // Get each controller device and build the mapping
        for (String deviceName : deviceNames) {
            DeviceMapping mapping = new DeviceMapping();

            {
                final CountDownLatch latch = new CountDownLatch(1);
                controller.getDevice(deviceName, new AsyncControllerCallback<org.openremote.entities.controller.Device>() {
                    @Override
                    public void onFailure(ControllerResponseCode controllerResponseCode) {
                        LOG.severe("Failed to get device '" + deviceName + "' from the controller: " + controllerResponseCode);
                        latch.countDown();
                    }

                    @Override
                    public void onSuccess(Device device) {
                        LOG.fine("Got device '" + deviceName + "' from the controller");
                        mapping.setDevice(device);
                        latch.countDown();
                    }
                });
                latch.await();
            }

            {
                final CountDownLatch latch = new CountDownLatch(1);
                // Register the device (so we get sensor change events and can send commands), this starts
                // the polling of sensor values - effectively long-lasting HTTP connection for each device!
                // TODO This will NOT scale! Well, depends on the internal implementation of our HTTP connector...
                if (mapping.getDevice() != null) {
                    mapping.setRegistrationHandle(
                        // TODO: Should be documented somewhere that this call blocks if the HTTP connector is not asynchronous
                        controller.registerDevice(mapping.getDevice(), new AsyncRegistrationCallback() {
                            @Override
                            public void onFailure(ControllerResponseCode controllerResponseCode) {
                                LOG.info("Failed to register device '" + deviceName + "' with the controller: " + controllerResponseCode);
                                latch.countDown();
                            }

                            @Override
                            public void onSuccess() {
                                LOG.fine("Registered device '" + deviceName + "' with the controller");
                                latch.countDown();
                            }
                        })
                    );
                    latch.await();
                }
            }

            mapping.update(widgetCommandInfos);
            onDeviceMappingUpdate.accept(mapping);
        }
    }

    protected void processSensorListener(SensorListener listener) {
        DeviceMapping deviceMapping = deviceMap.get(listener.getDeviceKey());

        if (deviceMapping == null) {
            LOG.fine("Device mapping for sensor listener cannot be found so ignoring, device key was: " + listener.getDeviceKey());
            return;
        }

        LOG.fine("Adding sensor listener to device mapping: " + listener.getDeviceKey());
        deviceMapping.addSensorListener(listener);
    }

    protected void announceDevices(DeviceListener listener) {
        LOG.fine("Announcing " + deviceMap.size() + " devices on listener: " + listener);
        deviceMap.values().stream().forEach(deviceMapping -> {
            listener.onDeviceAdded(deviceMapping.getDeviceAsset());
        });
    }

    protected void processWriteRequest(WriteResourceRequest request) {
        DeviceMapping deviceMapping = deviceMap.get(request.getDeviceKey());
        if (deviceMapping == null) {
            LOG.info("Request to write to unknown device key '" + request.getDeviceKey() + "' will be ignored");
            return;
        }

        DeviceResourceMapping resourceMapping = deviceMapping.getResourceMap().get(request.getResourceKey());
        if (resourceMapping == null) {
            LOG.info("Request to write to unknown resource key '" + request.getResourceKey() + "' will be ignored");
            return;
        }

        Command sendCommand = resourceMapping.getSendCommand1();

        Object resourceValue = request.getResourceValue();
        if (resourceValue != null && resourceMapping.getSendCommand2() != null) {
            // This is a switch the command to send varies depending on the value
            // (whoever invented this better not raise their hand...)
            switch (resourceValue.toString().toLowerCase(Locale.ROOT)) {
                case "off":
                case "false":
                    resourceValue = "off";
                    sendCommand = resourceMapping.getSendCommand2();
                    break;
                case "on":
                case "true":
                    resourceValue = "on";
                    break;
                default:
                    throw new IllegalArgumentException("Can't parse command value for boolean switch: " + resourceValue);
            }
        }

        if (sendCommand == null) {
            LOG.info("Unable to determine the command to send for " + request.getResourceValue() + " : " + request.getResourceKey());
            return;
        }

        LOG.fine("Sending command on '" + deviceMapping.getDevice().getName() + "': " + sendCommand.getName());
        // TODO: Callback handling? Should maybe just log something...
        deviceMapping.getDevice().sendCommand(
            sendCommand,
            (resourceValue != null ? resourceValue.toString() : null),
            new AsyncControllerCallback<CommandResponse>() {
                @Override
                public void onFailure(ControllerResponseCode controllerResponseCode) {
                    // TODO Components should offer a route for logging, so we can store (error) events
                }

                @Override
                public void onSuccess(CommandResponse commandResponse) {
                    // TODO Components should offer a route for logging, so we can store (succes) events
                }
            }
        );
    }

}
