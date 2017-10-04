package org.openremote.agent.protocol.knx;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.agent.protocol.ProtocolExecutorService;
import org.openremote.model.value.Value;

import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.DataUnitBuilder;
import tuwien.auto.calimero.DetachEvent;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXAckTimeoutException;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.datapoint.Datapoint;
import tuwien.auto.calimero.datapoint.DatapointMap;
import tuwien.auto.calimero.datapoint.DatapointModel;
import tuwien.auto.calimero.datapoint.StateDP;
import tuwien.auto.calimero.dptxlator.DPTXlator;
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
    
    protected final ProtocolExecutorService executorService;
    protected final int port = 3671;
    protected final String connectionType;
    protected KNXNetworkLink knxLink;
    protected ProcessCommunicator processCommunicator;
    protected DatapointModel<StateDP> datapoints = new DatapointMap<>();
    protected Map<StateDP, List<Consumer<Value>>> datapointValueConsumers = new HashMap<>();

    protected final String gatewayIp;
    
    private int remotePort;

    private boolean useNat;

    private String localKNXAddress;

    private String localIp;
    
    private static final Logger LOG = Logger.getLogger(KNXConnection.class.getName());
    
    public KNXConnection(String gatewayIp, String connectionType, ProtocolExecutorService executorService, String localIp, Integer remotePort, Boolean useNat, String localKNXAddress) {
        this.gatewayIp = gatewayIp;
        this.executorService = executorService;
        this.connectionType =  connectionType;
        this.localIp = localIp;
        this.remotePort = remotePort.intValue();
        this.useNat = useNat.booleanValue();
        this.localKNXAddress = localKNXAddress;
    }

    public synchronized void connect() {
        if (connectionStatus != ConnectionStatus.DISCONNECTED && connectionStatus != ConnectionStatus.WAITING) {
            LOG.finest("Must be disconnected before calling connect");
            return;
        }

        LOG.fine("Connecting");
        onConnectionStatusChanged(ConnectionStatus.CONNECTING);

        InetSocketAddress localEndPoint = null;
        InetSocketAddress remoteEndPoint = new InetSocketAddress(this.gatewayIp, this.remotePort);
        try {
            TPSettings tpSettings = new TPSettings(new IndividualAddress(this.localKNXAddress));
            if (StringUtils.isNotBlank(this.localIp)) {
                localEndPoint = new InetSocketAddress(this.localIp, 0);
            } else {
                InetAddress localHost = InetAddress.getLocalHost();
                localEndPoint = new InetSocketAddress(localHost, 0);
            }
            if (this.connectionType.equals("TUNNELLING")) {
                knxLink = KNXNetworkLinkIP.newTunnelingLink(localEndPoint, remoteEndPoint, this.useNat, tpSettings);
            } else {
                knxLink = KNXNetworkLinkIP.newRoutingLink(localEndPoint.getAddress(), remoteEndPoint.getAddress(), tpSettings);
            }
            
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
        catch (final KNXException | InterruptedException e) {
            LOG.log(Level.INFO, "Connection error", e.getMessage());
            scheduleReconnect();
        }
        catch (final UnknownHostException e) {
            LOG.log(Level.INFO, "Connection error", e.getMessage());
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
                LOG.fine("Sending to KNX action datapoint '" + datapoint + "': " + value);
                Value val = value.get();
                DPTXlator translator = TypeMapper.toDPTXlator(datapoint, val);
                processCommunicator.write(datapoint.getMainAddress(), translator);
            }
        } catch (KNXAckTimeoutException acke) {
            onConnectionStatusChanged(ConnectionStatus.CLOSED);
            processCommunicator.detach();
            knxLink.removeLinkListener(this);
            knxLink.close();
            knxLink = null;
            scheduleReconnect();
        } catch (Exception e) {
            LOG.severe(e.getMessage());
        }
    }
    
    public void groupWrite(final ProcessEvent e) { 
        if (datapoints.contains(e.getDestination())) {
            Datapoint datapoint = datapoints.get(e.getDestination());
            try {
                Value value = TypeMapper.toORValue(datapoint, e.getASDU());
                for (Consumer<Value> consumer : datapointValueConsumers.get(datapoint)) {
                    consumer.accept(value);
                }
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "Could translate KNX event: " + e, ex);
            }
        }
    }
    
    public void groupReadRequest(final ProcessEvent e) { groupWrite(e);  }
    
    public void groupReadResponse(final ProcessEvent e) { 
        groupWrite(e);
    }
    
    public void detached(final DetachEvent e) {}
    
    public void monitorStateDP(StateDP datapoint, Consumer<Value> consumer) {
        this.datapoints.add(datapoint);
        List<Consumer<Value>> consumers = this.datapointValueConsumers.get(datapoint);
        if (consumers == null) {
            consumers = new ArrayList<>();
            this.datapointValueConsumers.put(datapoint, consumers);
        }
        consumers.add(consumer);

        try {
            LOG.fine("Sending read request to KNX status datapoint '" + datapoint);
            this.knxLink.sendRequest(datapoint.getMainAddress(), datapoint.getPriority(), DataUnitBuilder.createLengthOptimizedAPDU(0x00, null));
        } catch (Exception e) {
            LOG.log(Level.INFO, "Error sending KNX read request for META_KNX_DPT: " + datapoint, e);
        }
    }
  
    public void stopMonitoringStateDP(StateDP datapoint) {
        this.datapoints.remove(datapoint);
    }
    
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
