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
package org.openremote.agent.controller2;

import org.openremote.console.controller.AsyncRegistrationCallback;
import org.openremote.console.controller.Controller;
import org.openremote.console.controller.ControllerConnectionStatus;
import org.openremote.console.controller.DeviceRegistrationHandle;
import org.openremote.console.controller.auth.UserPasswordCredentials;
import org.openremote.entities.controller.*;
import org.openremote.manager.shared.attribute.AttributeType;
import org.openremote.manager.shared.device.Device;
import org.openremote.manager.shared.device.DeviceResource;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Controller2Adapter {

    private static final Logger LOG = Logger.getLogger(Controller2Adapter.class.getName());

    /**
     * Lifecycle of a controller "session". The first endpoint in a route will open
     * the adapter. If all routes with endpoints are stopped, the adapter will be closed.
     */
    public interface Manager {
        Controller2Adapter openAdapter(URL url, String username, String password); // TODO: Add more options if needed
        void closeAdapter(Controller2Adapter adapter);
    }

    /**
     * This is a reference counting manager, one adapter for each adapter URL.
     */
    final static public Manager DEFAULT_MANAGER = new Manager() {
        final protected List<Controller2Adapter> adapters = new ArrayList<>();

        @Override
        public Controller2Adapter openAdapter(URL url, String username, String password) {
            // If adapter exists, increment reference count and return
            for (Controller2Adapter adapter : adapters) {
                if (adapter.getUrl().equals(url)) {
                    adapter.referenceCount++;
                    return adapter;
                }
            }
            Controller2Adapter adapter = new Controller2Adapter(url, username, password);
            adapters.add(adapter);
            return adapter;
        }

        @Override
        synchronized public void closeAdapter(Controller2Adapter adapter) {
            Iterator<Controller2Adapter> it = adapters.iterator();
            while (it.hasNext()) {
                Controller2Adapter next = it.next();
                if (next.getUrl().equals(adapter.getUrl())) {
                    // Count references down, if zero, close and remove
                    adapter.referenceCount--;
                    if (adapter.referenceCount == 0) {
                        adapter.close();
                        it.remove();
                    }
                    break;
                }
            }
        }
    };

    public interface DiscoveryListener {
        void onDiscovery(List<String> list); // TODO Use proper device type instead of strings?
    }

    public interface SensorListener {
        String getDeviceUri();
        String getResourceUri();
        void onUpdate(Object obj);
    }

    /**
     * Used by IOT connectors to announce changes to devices.
     * When a gateway component starts it should announce all devices
     * that it is already aware of and connected to.
     */
    public interface DeviceListener {
        void onDeviceAdded(Device device);

        void onDeviceRemoved(Device device);

        void onDeviceUpdated(Device device);
    }

    /**
     * Used to handle native sensor change callbacks and notify the component sensor listeners
     */
    protected static class SensorListenerHandler implements PropertyChangeListener {
        protected List<SensorListener> listeners;

        public synchronized void addListener(SensorListener listener) {
            if (listeners == null) {
                listeners = new ArrayList<>();
            }
            listeners.add(listener);
        }

        public synchronized void removeListener(SensorListener listener) {
            if (listeners != null) {
                listeners.remove(listener);
            }
        }

        @Override
        public synchronized void propertyChange(PropertyChangeEvent evt) {
            if (listeners == null) {
                return;
            }

            listeners.stream().forEach(sensorListener -> {
                sensorListener.onUpdate(evt.getNewValue());
            });
        }
    }

    /**
     * Contains the data to allow mapping between native command/sensor API and the component resource API
     */
    protected static class DeviceResourceMapping {
        protected DeviceResource resource;
        protected Sensor gatewaySensor;
        protected SensorListenerHandler sensorHandler;
        protected Command sendCommand1;
        protected Command sendCommand2;


        public synchronized void addSensorListener(SensorListener listener) {
            if (sensorHandler == null) {
                sensorHandler = new SensorListenerHandler();
            }

            sensorHandler.addListener(listener);
        }

        public synchronized void removeSensorListener(SensorListener listener) {
            if (sensorHandler == null) {
                return;
            }

            sensorHandler.removeListener(listener);
        }
    }

    /**
     * Contains the data to allow mapping between native device API and the component device API
     */
    protected static class DeviceMapping {
        protected org.openremote.entities.controller.Device gatewayDevice;
        protected Device device;
        protected DeviceRegistrationHandle registrationHandle;
        protected Map<String, DeviceResourceMapping> resourceMap = new HashMap<>();
    }

    /**
     * Used to track write requests which occur whilst connect/reconnect is occurring
     */
    public static class WriteResourceRequest {
        String deviceUri;
        String resourceUri;
        Object resourceValue;
    }

    protected volatile int referenceCount = 1; // Only remove adapter if no endpoint references it
    final URL url;
    final String username;
    final String password;
    final protected List<DiscoveryListener> discoveryListeners = new CopyOnWriteArrayList<>();
    final protected List<SensorListener> sensorListeners = new CopyOnWriteArrayList<>();
    final protected List<DeviceListener> deviceListeners = new CopyOnWriteArrayList<>();
    final protected Controller controller;
    protected boolean connectionInProgress;
    protected boolean forceDisconnect;
    final static protected long RECONNECT_DELAY_SECONDS = 5;
    // TODO: This will not scale, we can't hold 1 milliond device mappings in memory
    protected Map<String, DeviceMapping> deviceMap;
    protected final ScheduledExecutorService connectionScheduler = Executors.newScheduledThreadPool(1);
    protected List<WriteResourceRequest> queuedWriteRequests = new ArrayList<>();
    protected List<DeviceListener> queuedDeviceListeners = new ArrayList<>();
    protected final AsyncControllerCallback<ControllerConnectionStatus> connectCallback;

    public Controller2Adapter(URL url, String username, String password) {
        this(url, username, password, null);
    }

    public Controller2Adapter(URL url, String username, String password, Controller controller) {
        LOG.info("Creating adapter: " + url);
        this.url = url;
        this.username = username;
        this.password = password;

        if (controller == null) {
            String urlStr = url.toString();
            Controller.Builder cb = new Controller.Builder(urlStr);
            if (username != null && password != null) {
                cb.setCredentials(new UserPasswordCredentials(username, password));
            }
            controller = cb.build();
        }
        this.controller = controller;

        connectCallback = new AsyncControllerCallback<ControllerConnectionStatus>() {

            @Override
            public void onFailure(ControllerResponseCode controllerResponseCode) {
                LOG.fine("Disconnected from the controller: " + url);
                if (!forceDisconnect) {
                    doConnection(true);
                }
            }

            @Override
            public void onSuccess(ControllerConnectionStatus controllerConnectionStatus) {
                LOG.fine("Connected to the controller: " + url);
                if (!forceDisconnect) {
                    onConnected();
                }
            }
        };

        // Whenever a connection problem occurs the controller library will disconnect and call the connect
        // onFailure callback with a disconnected status; we then need to handle reconnection
        doConnection(false);
    }

    public URL getUrl() {
        return url;
    }

    public synchronized void close() {
        LOG.info("Closing adapter: " + url);
        if (controller != null) {
            forceDisconnect = true;
            controller.disconnect();
        }
    }

    public synchronized void addDiscoveryListener(DiscoveryListener listener) {
        LOG.fine("Adding discovery listener (" + url + "): " + listener);
        discoveryListeners.add(listener);
    }

    public synchronized void removeDiscoveryListener(DiscoveryListener listener) {
        LOG.fine("Removing discovery listener (" + url + "): " + listener);
        discoveryListeners.remove(listener);
    }

    public void triggerDiscovery() {
        LOG.fine("Triggering discovery (" + url + ")");
        // TODO: This should, at some point (asynchronous?) call the registered discovery listeners
    }

    public synchronized void addSensorListener(SensorListener listener) {
        LOG.fine("Adding sensor listener (" + url + "): " + listener);
        sensorListeners.add(listener);

        if (connectionInProgress || controller == null || !controller.isConnected()) {
            return;
        }

        processSensorListener(listener);
    }

    protected void processSensorListener(SensorListener listener) {
        // Register the handler
        DeviceMapping deviceMapping = deviceMap.get(listener.getDeviceUri());

        if (deviceMapping == null) {
            LOG.fine("Device URI for sensor listener cannot be found so ignoring");
            return;
        }

        DeviceResourceMapping resourceMapping = deviceMapping.resourceMap.get(listener.getResourceUri());

        if (resourceMapping == null) {
            LOG.fine("Resource URI for sensor listener cannot be found so ignoring");
            return;
        }

        resourceMapping.addSensorListener(listener);
    }

    public synchronized void removeSensorListener(SensorListener listener) {
        LOG.fine("Removing sensor listener (" + url + "): " + listener);
        sensorListeners.remove(listener);

        if (deviceMap == null) {
            return;
        }

        // Unregister the handler
        DeviceMapping deviceMapping = deviceMap.get(listener.getDeviceUri());

        if (deviceMapping == null) {
            LOG.fine("Device URI for sensor listener cannot be found so ignoring");
            return;
        }

        DeviceResourceMapping resourceMapping = deviceMapping.resourceMap.get(listener.getResourceUri());

        if (resourceMapping == null) {
            LOG.fine("Resource URI for sensor listener cannot be found so ignoring");
            return;
        }

        resourceMapping.removeSensorListener(listener);
    }

    public synchronized void writeResource(String deviceUri, String resourceUri, Object resourceValue) {
        LOG.fine("Writing resource (" + url + "): " + deviceUri + " : " + resourceUri + " - " + resourceValue);

        if (connectionInProgress || controller == null || !controller.isConnected()) {
            WriteResourceRequest writeRequest = new WriteResourceRequest();
            writeRequest.deviceUri = deviceUri;
            writeRequest.resourceUri = resourceUri;
            writeRequest.resourceValue = resourceValue;
            queuedWriteRequests.add(writeRequest);
            return;
        }

        processWriteRequest(deviceUri, resourceUri, resourceValue);
    }

    public synchronized void addDeviceListener(DeviceListener listener) {
        LOG.fine("Adding device listener (" + url + "): " + listener);
        deviceListeners.add(listener);

        if (connectionInProgress || controller == null || !controller.isConnected()) {
            queuedDeviceListeners.add(listener);
            return;
        }

        announceDevices(listener);
    }

    public synchronized void removeDeviceListener(DeviceListener listener) {
        LOG.fine("Removing device listener (" + url + "): " + listener);
        deviceListeners.remove(listener);
    }

    private synchronized void doConnection(boolean doDelay) {
        if (connectionInProgress || controller.isConnected()) {
            return;
        }

        connectionInProgress = true;
        // Push connection task onto separate thread to avoid blocking the caller when delaying reconnection
        connectionScheduler.schedule(() -> {
            controller.connect(connectCallback);
        }, (doDelay ? RECONNECT_DELAY_SECONDS : 0L), TimeUnit.SECONDS);
    }

    protected synchronized void onConnected() {
        // TODO: Update the controller library to return futures
        // TODO: Resync the devices after disconnect/reconnect
        if (deviceMap == null) {
            List<String> deviceNames = new ArrayList<>();
            List<Controller.WidgetCommandInfo> widgetCommandInfos = new ArrayList<>();

            try {
                Semaphore doneSignal = new Semaphore(1);

                deviceMap = new HashMap<>();

                // Get widget command information to allow capability
                // construction and writable resource resolution
                controller.getWidgetsCommandsInfo(new AsyncControllerCallback<List<Controller.WidgetCommandInfo>>() {
                    @Override
                    public void onFailure(ControllerResponseCode controllerResponseCode) {
                        LOG.info("Failed to get widget command info from the controller: " + controllerResponseCode);
                        doneSignal.release();
                    }

                    @Override
                    public void onSuccess(List<Controller.WidgetCommandInfo> result) {
                        LOG.fine("Got widget command info from controller");
                        widgetCommandInfos.addAll(result);
                        doneSignal.release();
                    }
                });

                doneSignal.acquire();

                // Get device names available on this controller
                controller.getDeviceList(new AsyncControllerCallback<List<DeviceInfo>>() {
                    @Override
                    public void onFailure(ControllerResponseCode controllerResponseCode) {
                        LOG.info("Failed to get device list from the controller: " + controllerResponseCode);
                        doneSignal.release();
                    }

                    @Override
                    public void onSuccess(List<DeviceInfo> deviceInfos) {
                        LOG.fine("Got device list from controller");
                        deviceNames.addAll(deviceInfos.stream().map(deviceInfo -> deviceInfo.getName()).collect(Collectors.toList()));
                        doneSignal.release();
                    }
                });

                doneSignal.acquire();

                // Get each controller device and build the mapping
                for (String deviceName : deviceNames) {
                    DeviceMapping mapping = new DeviceMapping();

                    controller.getDevice(deviceName, new AsyncControllerCallback<org.openremote.entities.controller.Device>() {
                        @Override
                        public void onFailure(ControllerResponseCode controllerResponseCode) {
                            LOG.info("Failed to get device '" + deviceName + "' from the controller: " + controllerResponseCode);
                            doneSignal.release();
                        }

                        @Override
                        public void onSuccess(org.openremote.entities.controller.Device gatewayDevice) {
                            LOG.fine("Got device '" + deviceName + "' from the controller");
                            mapping.gatewayDevice = gatewayDevice;
                            doneSignal.release();
                        }
                    });

                    doneSignal.acquire();

                    // Register the gtaeway device (so we get sensor change events and can send commands)
                    if (mapping.gatewayDevice != null) {
                        mapping.registrationHandle = controller.registerDevice(mapping.gatewayDevice, new AsyncRegistrationCallback() {
                            @Override
                            public void onFailure(ControllerResponseCode controllerResponseCode) {
                                LOG.info("Failed to register device '" + deviceName + "' with the controller: " + controllerResponseCode);
                                doneSignal.release();
                            }

                            @Override
                            public void onSuccess() {
                                LOG.fine("Registered device '" + deviceName + "' with the controller");
                                doneSignal.release();
                            }
                        });

                        doneSignal.acquire();
                    }

                    deviceMap.put(deviceName.toLowerCase(), mapping);
                    updateDeviceMapping(mapping, widgetCommandInfos);
                }

            } catch (InterruptedException e) {
                LOG.severe("Device synchronisation was interrupted");
            }

            // Process sensor listeners added before device map was constructed
            sensorListeners.forEach(this::processSensorListener);
        }

        connectionInProgress = false;

        // process any new device listeners
        queuedDeviceListeners.stream().forEach(this::announceDevices);
        queuedDeviceListeners.clear();

        // process any new commands
        queuedWriteRequests.stream().forEach(request -> processWriteRequest(request.deviceUri, request.resourceUri, request.resourceValue));
        queuedWriteRequests.clear();
    }

    protected void updateDeviceMapping(DeviceMapping deviceMapping, List<Controller.WidgetCommandInfo> widgetCommandInfos) {
        deviceMapping.device = null;
        org.openremote.entities.controller.Device gatewayDevice = deviceMapping.gatewayDevice;

        if (gatewayDevice != null) {
            Device device = new Device();
            deviceMapping.device = device;
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
                dr.setUri(sensor.getName().toLowerCase());
                DeviceResourceMapping resourceMapping = new DeviceResourceMapping();
                resourceMapping.resource = dr;
                resourceMapping.gatewaySensor = sensor;
                resources.add(dr);
                deviceMapping.resourceMap.put(dr.getUri(), resourceMapping);

                if (matchingWidgetInfo != null) {
                    if (matchingWidgetInfo.getCommandId1() > 0) {
                        assignedCommands.add(matchingWidgetInfo.getCommandId1());
                        resourceMapping.sendCommand1 = gatewayDevice.getCommands().stream()
                                .filter(command -> command.getId() == matchingWidgetInfo.getCommandId1())
                                .findFirst()
                                .orElseGet(() -> null);
                    }
                    if (matchingWidgetInfo.getCommandId2() > 0) {
                        resourceMapping.sendCommand2 = gatewayDevice.getCommands().stream()
                                .filter(command -> command.getId() == matchingWidgetInfo.getCommandId2())
                                .findFirst()
                                .orElseGet(() -> null);
                        assignedCommands.add(matchingWidgetInfo.getCommandId2());
                    }
                }

                switch(sensor.getType()) {
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
                dr.setUri(command.getName().toLowerCase());
                dr.setType(null);
                dr.setAccess(DeviceResource.Access.W);

                DeviceResourceMapping resourceMapping = new DeviceResourceMapping();
                resourceMapping.resource = dr;
                resourceMapping.sendCommand1 = command;
                resources.add(dr);
                deviceMapping.resourceMap.put(dr.getUri(), resourceMapping);
            });

            device.setResources(resources.toArray(new DeviceResource[resources.size()]));
        }
    }

    protected void announceDevices(DeviceListener listener) {
        LOG.fine("Announcing devices");
        deviceMap.values().stream().forEach(deviceMapping -> {
            listener.onDeviceAdded(deviceMapping.device);
        });
    }

    protected void processWriteRequest(String deviceUri, String resourceUri, Object resourceValue) {
        if (deviceMap == null) {
            return;
        }

        DeviceMapping deviceMapping = deviceMap.get(deviceUri);

        if (deviceMapping == null) {
            LOG.info("Request to write to unknown device URI '" + deviceUri + "' will be ignored");
            return;
        }

        DeviceResourceMapping resourceMapping = deviceMapping.resourceMap.get(resourceUri);

        if (resourceMapping == null) {
            LOG.info("Request to write to unknown resource URI '" + resourceUri + "' will be ignored");
            return;
        }

        Command sendCommand = resourceMapping.sendCommand1;

        if (resourceMapping.sendCommand2 != null) {
            // This is a switch the command to send varies depending on the value
            boolean switchValue = (boolean)resourceValue;
            if (!switchValue) {
                // Use off command which is command2
                sendCommand = resourceMapping.sendCommand2;
            }
            resourceValue = switchValue ? "on" : "off";
        }

        if (sendCommand == null) {
            LOG.info("Unable to determine the command to send for " + deviceUri + " : " + resourceUri);
            return;
        }

        // TODO: Callback handling? Should maybe just log something...
        deviceMapping.gatewayDevice.sendCommand(sendCommand, (resourceValue != null ? resourceValue.toString() : null), new AsyncControllerCallback<CommandResponse>() {
            @Override
            public void onFailure(ControllerResponseCode controllerResponseCode) {

            }

            @Override
            public void onSuccess(CommandResponse commandResponse) {

            }
        });
    }
}
