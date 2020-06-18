package org.openremote.agent.protocol.tradfri;

import org.openremote.agent.protocol.tradfri.device.Device;
import org.openremote.agent.protocol.tradfri.device.Gateway;
import org.openremote.agent.protocol.tradfri.device.Light;
import org.openremote.agent.protocol.tradfri.device.Plug;
import org.openremote.agent.protocol.tradfri.util.Credentials;

import org.openremote.agent.protocol.ProtocolExecutorService;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.syslog.SyslogCategory;

import java.util.function.Consumer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

/**
 * The class that represents the configuration for the IKEA TRÅDFRI connection.
 */
public class TradfriConnection {

    /**
     * The connection status, the default is disconnected
     */
    protected ConnectionStatus connectionStatus = ConnectionStatus.DISCONNECTED;

    /**
     * The connection status consumers of the IKEA TRÅDFRI connection
     */
    protected final List<Consumer<ConnectionStatus>> connectionStatusConsumers = new ArrayList<>();

    /**
     * The executor service of the IKEA TRÅDFRI connection
     */
    protected final ProtocolExecutorService executorService;

    /**
     * The IP address of the gateway
     */
    protected final String gatewayIp;

    /**
     * The security code that's needed to connect to the gateway.
     */
    protected final String securityCode;

    /**
     * The logger for the IKEA TRÅDFRI connection.
     */
    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, TradfriConnection.class);

    /**
     * The IKEA TRÅDFRI gateway.
     */
    private Gateway gateway;

    /**
     * Construct the TradfriConnection class.
     * @param gatewayIp the IP address of the gateway.
     * @param securityCode the security code to connect to the gateway.
     * @param executorService the executor service.
     */
    public TradfriConnection(String gatewayIp, String securityCode, ProtocolExecutorService executorService) {
        this.gatewayIp = gatewayIp;
        this.securityCode = securityCode;
        this.executorService = executorService;
    }

    /**
     * Handles the connection.
     * @return the gateway.
     */
    public synchronized Gateway connect() {
        if (connectionStatus == ConnectionStatus.CONNECTED || connectionStatus == ConnectionStatus.CONNECTING) {
            LOG.finest("Already connected or connection in progress");
        }

        onConnectionStatusChanged(ConnectionStatus.CONNECTING);

        try {
            Gateway gateway = new Gateway(gatewayIp);
            gateway.setTimeout(10000L);
            Credentials credentials = gateway.connect(securityCode);
            if (credentials != null) {
                if (gateway.enableObserve()) {
                    onConnectionStatusChanged(ConnectionStatus.CONNECTED);
                    this.gateway = gateway;
                    return gateway;
                }
            }
        }
        catch (Exception exception) {
            LOG.warning("An exception occured when connecting to Tradfri gateway: " + exception);
        }
        return null;
    }

    /**
     * Updates the connection status.
     * @param connectionStatus the connection status.
     */
    protected synchronized void onConnectionStatusChanged(ConnectionStatus connectionStatus) {
        this.connectionStatus = connectionStatus;

        connectionStatusConsumers.forEach(
                consumer -> consumer.accept(connectionStatus)
        );
    }

    /**
     * Adds a connection status consumer.
     * @param connectionStatusConsumer the connection status consumer.
     */
    public synchronized void addConnectionStatusConsumer(Consumer<ConnectionStatus> connectionStatusConsumer) {
        if (!connectionStatusConsumers.contains(connectionStatusConsumer)) {
            connectionStatusConsumers.add(connectionStatusConsumer);
        }
    }

    /**
     * Handles the disconnection.
     */
    public synchronized void disconnect() {
        if (connectionStatus == ConnectionStatus.DISCONNECTING || connectionStatus == ConnectionStatus.DISCONNECTED) {
            LOG.finest("Already disconnecting or disconnected");
            return;
        }
        LOG.finest("Disconnecting");
        onConnectionStatusChanged(ConnectionStatus.DISCONNECTING);
        if(gateway.disableObserve()){
            onConnectionStatusChanged(ConnectionStatus.DISCONNECTED);
        }
    }

    /**
     * Removes the connection status consumer.
     * @param connectionStatusConsumer the connection status consumer.
     */
    public synchronized void removeConnectionStatusConsumer(java.util.function.Consumer<ConnectionStatus> connectionStatusConsumer) {
        connectionStatusConsumers.remove(connectionStatusConsumer);
    }

    /**
     * Method to update the values of the device attributes,
     * based on the values entered in the User Interface.
     * @param device the device to which the update applies.
     * @param event the event representing the update.
     */
    public void controlDevice(Device device, AttributeEvent event) {
        try {
            if (this.connectionStatus == ConnectionStatus.CONNECTED && event.getValue().isPresent()) {
                if (device.isLight()){
                    Light light = device.toLight();
                    switch (event.getAttributeName()) {
                        case "lightDimLevel":
                            light.setBrightness((int) Float.parseFloat(event.getValue().get().toString()));
                            break;
                        case "lightStatus":
                            light.setOn(Boolean.parseBoolean(event.getValue().get().toString()));
                            break;
                        case "colorGBW":
                            String digits = event.getValue().get().toString().replaceAll("[^0-9.,]+","");
                            String[] numbers = digits.split(",");
                            light.setColourRGB(Integer.parseInt(numbers[0]), Integer.parseInt(numbers[1]), Integer.parseInt(numbers[2]));
                            break;
                        case "colorTemperature":
                            light.setColourTemperature((int) Float.parseFloat(event.getValue().get().toString()));
                            break;
                    }
                }
                else if (device.isPlug()) {
                    Plug plug = device.toPlug();
                    switch (event.getAttributeName()){
                        case "plugOnOrOff":
                            plug.setOn(Boolean.parseBoolean(event.getValue().get().toString()));
                            break;
                    }
                }
            }
        } catch (Exception exception) {
            LOG.severe(exception.getMessage());
        }
    }
}
