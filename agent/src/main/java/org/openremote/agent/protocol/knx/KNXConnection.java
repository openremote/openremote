package org.openremote.agent.protocol.knx;

import org.apache.commons.lang3.StringUtils;
import org.openremote.agent.protocol.ProtocolExecutorService;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.util.Pair;
import org.openremote.model.value.Value;
import tuwien.auto.calimero.*;
import tuwien.auto.calimero.datapoint.Datapoint;
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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    protected final Map<GroupAddress, byte[]> groupAddressStateMap = new HashMap<>();
    protected final Map<GroupAddress, List<Pair<StateDP, Consumer<Value>>>> groupAddressConsumerMap = new HashMap<>();

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
        this.remotePort = remotePort;
        this.useNat = useNat;
        this.localKNXAddress = localKNXAddress;
    }

    public synchronized void connect() {
        if (connectionStatus == ConnectionStatus.CONNECTED || connectionStatus == ConnectionStatus.CONNECTING) {
            LOG.finest("Already connected or connection in progress");
            return;
        }

        onConnectionStatusChanged(ConnectionStatus.CONNECTING);

        InetSocketAddress localEndPoint;
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

                // Get the values of all registered group addresses
                LOG.fine("Initialising group address values");
                synchronized (groupAddressConsumerMap) {
                    groupAddressConsumerMap.forEach((groupAddress, datapointConsumerList) -> {
                        if (!datapointConsumerList.isEmpty()) {
                            // Take first data point for the group address and request the value
                            Pair<StateDP, Consumer<Value>> datapointConsumer = datapointConsumerList.get(0);
                            getGroupAddressValue(datapointConsumer.key.getMainAddress(), datapointConsumer.key.getPriority());
                        }
                    });
                }

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

    protected synchronized void onConnectionStatusChanged(ConnectionStatus connectionStatus) {
        this.connectionStatus = connectionStatus;

        connectionStatusConsumers.forEach(
            consumer -> consumer.accept(connectionStatus)
        );
    }
    
    public void sendCommand(Datapoint datapoint, Optional<Value> value) {
        try {
            if (this.connectionStatus == ConnectionStatus.CONNECTED && value.isPresent()) {
                LOG.fine("Sending to KNX action datapoint '" + datapoint + "': " + value);
                Value val = value.get();
                DPTXlator translator = TypeMapper.toDPTXlator(datapoint, val);
                processCommunicator.write(datapoint.getMainAddress(), translator);
            }
        } catch (KNXAckTimeoutException e) {
            LOG.log(Level.INFO, "Failed to send KNX value: " + datapoint + " : " + value, e);
            onConnectionError();
        } catch (Exception e) {
            LOG.severe(e.getMessage());
        }
    }

    /**
     * A group address has changed on the KNX network so notify consumers
     */
    @Override
    public void groupWrite(final ProcessEvent e) {
        onGroupAddressUpdated(e.getDestination(), e.getASDU());
    }

    protected void onGroupAddressUpdated(GroupAddress groupAddress, byte[] value) {
        synchronized (groupAddressStateMap) {
            // Update the state map and notify consumers
            groupAddressStateMap.compute(groupAddress, (ga, oldValue) -> value);
        }

        synchronized (groupAddressConsumerMap) {
            groupAddressConsumerMap.computeIfPresent(groupAddress, (ga, datapointAndConsumerList) -> {
                datapointAndConsumerList.forEach(datapointAndConsumer -> {
                    StateDP datapoint = datapointAndConsumer.key;
                    Consumer<Value> consumer = datapointAndConsumer.value;
                    updateConsumer(value, datapoint, consumer);
                });

                return datapointAndConsumerList;
            });
        }
    }

    @Override
    public void groupReadRequest(final ProcessEvent e) {
        // RT: From description of super method I don't think we should update internal state in response to this but could be wrong
        //groupWrite(e);
    }

    @Override
    public void groupReadResponse(final ProcessEvent e) {
        groupWrite(e);
    }

    @Override
    public void detached(final DetachEvent e) {
        LOG.log(Level.INFO, "KNX link detached", e.getSource());
    }


    @Override
    public void indication(FrameEvent e) {
    }

    @Override
    public void linkClosed(CloseEvent e) {
        LOG.log(Level.INFO, "KNX link closed", e.getReason());
        // RT: Is this called when deliberately disconnecting or just when an error occurs?
        onConnectionError();
    }

    @Override
    public void confirmation(FrameEvent e) {
    }

    protected void onConnectionError() {
        onConnectionStatusChanged(ConnectionStatus.ERROR);
        processCommunicator.detach();
        if (knxLink != null) {
            knxLink.removeLinkListener(this);
            knxLink.close();
        }
        knxLink = null;

        // Clear out the group address states
        List<GroupAddress> groupAddresses = Arrays.asList(groupAddressStateMap.keySet().toArray(new GroupAddress[groupAddressStateMap.size()]));
        groupAddresses.forEach(groupAddress -> onGroupAddressUpdated(groupAddress, null));

        scheduleReconnect();
    }

    /**
     * Add a consumer for the specified {@link StateDP}.
     */
    public void addDatapointValueConsumer(StateDP datapoint, Consumer<Value> consumer) {
        synchronized (groupAddressConsumerMap) {
            List<Pair<StateDP, Consumer<Value>>> groupAddressConsumers = groupAddressConsumerMap
                .computeIfAbsent(datapoint.getMainAddress(), groupAddress -> new ArrayList<>());

            groupAddressConsumers.add(new Pair<>(datapoint, consumer));

            // Look for existing value for this GA
            synchronized (groupAddressStateMap) {
                groupAddressStateMap.compute(datapoint.getMainAddress(), (groupAddress, groupValue) -> {
                    if (groupValue == null) {
                        // State not available for this group address so request it
                        getGroupAddressValue(datapoint.getMainAddress(), datapoint.getPriority());
                    } else {
                        updateConsumer(groupValue, datapoint, consumer);
                    }

                    return groupValue;
                });
            }
        }
    }

    /**
     * Remove a consumer for the specified {@link StateDP}.
     * <p>
     * <b>NOTE: The {@link StateDP} must be the same instance as supplied at registration.</b>
     */
    public void removeDatapointValueConsumer(StateDP datapoint) {
        synchronized (groupAddressConsumerMap) {
            groupAddressConsumerMap.computeIfPresent(datapoint.getMainAddress(), (groupAddress, datapointConsumerList) -> {
                if (datapointConsumerList.removeIf(datapointConsumer -> datapointConsumer.key == datapoint)) {
                    if (datapointConsumerList.isEmpty()) {
                        datapointConsumerList = null;
                    }
                }
                return datapointConsumerList;
            });
        }
    }

    protected void getGroupAddressValue(GroupAddress groupAddress, Priority priority) {
        if (knxLink == null || !knxLink.isOpen()) {
            LOG.fine("Cannot send read request not currently connected: " + groupAddress);
            return;
        }

        try {
            LOG.fine("Sending read request to KNX group address: " + groupAddress);
            this.knxLink.sendRequest(groupAddress, priority, DataUnitBuilder.createLengthOptimizedAPDU(0x00, null));
        } catch (Exception e) {
            LOG.log(Level.INFO, "Error sending KNX read request for group address: " + groupAddress, e);
        }
    }

    protected void updateConsumer(byte[] data, StateDP datapoint, Consumer<Value> consumer) {
        // Convert to OR Value and notify the consumer
        Value value = null;

        if (data != null) {
            try {
                value = TypeMapper.toORValue(datapoint, data);
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "Couldn't translate Group address value to DPT type: " + datapoint, ex);
            }
        }

        consumer.accept(value);
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
