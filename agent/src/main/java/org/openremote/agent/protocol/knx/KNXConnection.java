package org.openremote.agent.protocol.knx;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openremote.agent.protocol.ConnectionStatus;
import org.openremote.agent.protocol.ProtocolExecutorService;
import org.openremote.model.value.BooleanValue;
import org.openremote.model.value.NumberValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueException;

import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.DetachEvent;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.datapoint.Datapoint;
import tuwien.auto.calimero.link.KNXNetworkLink;
import tuwien.auto.calimero.link.KNXNetworkLinkIP;
import tuwien.auto.calimero.link.NetworkLinkListener;
import tuwien.auto.calimero.link.medium.TPSettings;
import tuwien.auto.calimero.process.ProcessCommunicator;
import tuwien.auto.calimero.process.ProcessCommunicatorImpl;
import tuwien.auto.calimero.process.ProcessEvent;
import tuwien.auto.calimero.process.ProcessListener;

public class KNXConnection implements NetworkLinkListener, ProcessListener {

    protected ConnectionStatus connectionStatus = ConnectionStatus.DISCONNECTED;
    
    protected final static int INITIAL_RECONNECT_DELAY_MILLIS = 1000;
    protected final static int MAX_RECONNECT_DELAY_MILLIS = 60000;
    protected final static int RECONNECT_BACKOFF_MULTIPLIER = 2;
    protected ScheduledFuture<?> reconnectTask;
    protected int reconnectDelayMilliseconds = INITIAL_RECONNECT_DELAY_MILLIS;
    
    protected final List<Consumer<ConnectionStatus>> connectionStatusConsumers = new ArrayList<>();
    
    protected final String gatewayIp;
    protected final ProtocolExecutorService executorService;
    protected final int port = 3671;
    protected KNXNetworkLink knxLink;
    protected ProcessCommunicator processCommunicator;
    
    private static final Logger LOG = Logger.getLogger(KNXConnection.class.getName());
    
    public KNXConnection(String gatewayIp, ProtocolExecutorService executorService) {
        this.gatewayIp = gatewayIp;
        this.executorService = executorService;
    }

    public synchronized void connect() {
        if (connectionStatus != ConnectionStatus.DISCONNECTED && connectionStatus != ConnectionStatus.WAITING) {
            LOG.finest("Must be disconnected before calling connect");
            return;
        }

        LOG.fine("Connecting");
        onConnectionStatusChanged(ConnectionStatus.CONNECTING);

        
        final InetSocketAddress remote = new InetSocketAddress(gatewayIp, port);
        try  {
            //TODO accept local address, local port, useNat as extra connection params from protocl configuration
            //final InetSocketAddress localSocket = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
            knxLink = KNXNetworkLinkIP.newTunnelingLink(null, remote, false, TPSettings.TP1);
            
            if (knxLink.isOpen()) {
                LOG.fine("Successfully connected to: " + gatewayIp + ":" + port);
                processCommunicator = new ProcessCommunicatorImpl(knxLink);
                processCommunicator.addProcessListener(this);
                knxLink.addLinkListener(this);

                reconnectTask = null;
                reconnectDelayMilliseconds = INITIAL_RECONNECT_DELAY_MILLIS;
                onConnectionStatusChanged(ConnectionStatus.CONNECTED);
            } else {
                LOG.log(Level.INFO, "Connection error");
                // Failed to connect so schedule reconnection attempt
                scheduleReconnect();
            }
        }
        catch (final KNXException | InterruptedException | RuntimeException e) {
            LOG.log(Level.INFO, "Connection error", e.getMessage());
            scheduleReconnect();
        }
    }

    public synchronized void disconnect() {
        if (connectionStatus == ConnectionStatus.DISCONNECTING || connectionStatus == ConnectionStatus.DISCONNECTED) {
            LOG.finest("Already disconnecting or disconnected");
            return;
        }

        LOG.finest("Disconnecting");
        onConnectionStatusChanged(ConnectionStatus.DISCONNECTING);
        if (processCommunicator != null) {
            processCommunicator.detach();
        }
        if (knxLink != null) {
            knxLink.removeLinkListener(this);
            knxLink.close();
            knxLink = null;
        }
        onConnectionStatusChanged(ConnectionStatus.DISCONNECTED);
    }
        
    public synchronized void addConnectionStatusConsumer(Consumer<ConnectionStatus> connectionStatusConsumer) {
        if (!connectionStatusConsumers.contains(connectionStatusConsumer)) {
            connectionStatusConsumers.add(connectionStatusConsumer);
        }
    }

    public synchronized void removeConnectionStatusConsumer(Consumer<ConnectionStatus> connectionStatusConsumer) {
        connectionStatusConsumers.remove(connectionStatusConsumer);
    }

    protected void onConnectionStatusChanged(ConnectionStatus connectionStatus) {
        this.connectionStatus = connectionStatus;

        synchronized (connectionStatusConsumers) {
            connectionStatusConsumers.forEach(
                consumer -> consumer.accept(connectionStatus)
            );
        }
    }
    
    public void sendCommand(Datapoint datapoint, Optional<Value> value) {
        try {
            if (this.connectionStatus == ConnectionStatus.CONNECTED && value.isPresent()) {
                //TODO a more detailed conversion of value to needed KNX datapoint value
                Value val = value.get();
                switch (val.getType()) {
                    case BOOLEAN:
                        processCommunicator.write(datapoint.getMainAddress(), ((BooleanValue)val).getBoolean());    
                        break;
                    case NUMBER:
                        processCommunicator.write(datapoint.getMainAddress(),(float) ((NumberValue)val).getNumber(), datapoint.getMainNumber() == 14);
                        break;
                    default:
                        break;
                }
            }
        } catch (ValueException | KNXException e) {
            LOG.severe(e.getMessage());
        }
    }
    
    public void groupWrite(final ProcessEvent e) { 
        //TODO listen on groupWrites to statusGA and update attribute
    }
    
    public void groupReadRequest(final ProcessEvent e) {  }
    
    public void groupReadResponse(final ProcessEvent e) { 
      //TODO listen on readResponses to statusGA and update attribute
    }
    
    public void detached(final DetachEvent e) {}
    
//TODO  when registering statusGA send readRequest for initial status
//    public synchronized MeasurementRegistration monitorReadGroupAddress(int deviceId, String measurementName, BiConsumer<MeasurementRegistration, Measurement> consumer) {
//        // Enervalis API v1 cannot provide device info for a list of devices by ID
//        // (devices would need to be grouped into a device group first) - instead lets
//        // just poll all devices that this user has access to and extract the ones we're
//        // interested in - may not be ideal but let's assume we're going to want to monitor
//        // most of the devices.
//
//        // Normalise the measurement name (case insensitive)
//        measurementName = measurementName.toLowerCase().trim();
//
//        // Build registration hash key
//        String regKey = deviceId + "_" + measurementName;
//
//        // Build registration
//        Datapoint datapoint = new StateD (deviceId, measurementName, consumer);
//
//        List<MeasurementRegistration> registrations =
//            measurementRegistrations.computeIfAbsent(regKey, k -> new ArrayList<>());
//        registrations.add(registration);
//
//        startDevicePolling();
//        return registration;
//    }
    
    
    @Override
    public void indication(FrameEvent e) {
    }

    @Override
    public void linkClosed(CloseEvent e) {
        LOG.log(Level.INFO, "KNX link closed", e.getReason());
        onConnectionStatusChanged(ConnectionStatus.CLOSED);
        processCommunicator.detach();
        knxLink.removeLinkListener(this);
        knxLink.close();
        knxLink = null;
        scheduleReconnect();
    }

    @Override
    public void confirmation(FrameEvent e) {
    }
    
    protected synchronized void scheduleReconnect() {
        if (reconnectTask != null) {
            return;
        }

        onConnectionStatusChanged(ConnectionStatus.WAITING);

        if (reconnectDelayMilliseconds < MAX_RECONNECT_DELAY_MILLIS) {
            reconnectDelayMilliseconds *= RECONNECT_BACKOFF_MULTIPLIER;
            reconnectDelayMilliseconds = Math.min(MAX_RECONNECT_DELAY_MILLIS, reconnectDelayMilliseconds);
        }

        LOG.finest("Scheduling reconnection in '" + reconnectDelayMilliseconds + "' milliseconds");

        reconnectTask = executorService.schedule(() -> {
            synchronized (KNXConnection.this) {
                reconnectTask = null;
                // Attempt to reconnect if not disconnecting
                if (connectionStatus != ConnectionStatus.DISCONNECTING && connectionStatus != ConnectionStatus.DISCONNECTED) {
                    connect();
                }
            }
        }, reconnectDelayMilliseconds);
    }
}
