package org.openremote.agent.protocol.knx;

import static org.openremote.model.Constants.PROTOCOL_NAMESPACE;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Logger;

import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.agent.protocol.ConnectionStatus;
import org.openremote.model.AbstractValueHolder;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.util.Pair;

import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.datapoint.CommandDP;
import tuwien.auto.calimero.datapoint.Datapoint;
import tuwien.auto.calimero.datapoint.StateDP;

public class KNXProtocol extends AbstractProtocol {

    private static final Logger LOG = Logger.getLogger(KNXProtocol.class.getName());

    public static final String PROTOCOL_NAME = PROTOCOL_NAMESPACE + ":knx";
    public static final String KNX_GATEWAY_IP = PROTOCOL_NAME + ":gatewayIp";
    public static final String DPT = PROTOCOL_NAME + ":dpt";
    public static final String STATUS_GA = PROTOCOL_NAME + ":statusGA";
    public static final String ACTION_GA = PROTOCOL_NAME + ":actionGA";

    final protected Map<String, KNXConnection> knxConnections = new HashMap<>();
    final protected Map<AttributeRef, Consumer<ConnectionStatus>> statusConsumerMap = new HashMap<>();
    final protected Map<AttributeRef, Pair<KNXConnection, Datapoint>> attributeActionMap = new HashMap<>();
    final protected Map<AttributeRef, Pair<KNXConnection, Datapoint>> attributeStatusMap = new HashMap<>();
    
    @Override
    public String getProtocolName() {
        return PROTOCOL_NAME;
    }

    @Override
    protected void doLinkProtocolConfiguration(AssetAttribute protocolConfiguration) {
        Optional<String> gatewayIpParam = protocolConfiguration.getMetaItem(KNX_GATEWAY_IP).flatMap(AbstractValueHolder::getValueAsString);
        if (!gatewayIpParam.isPresent()) {
            LOG.severe("No KNX gateway IP address provided for protocol configuration: " + protocolConfiguration);
            updateStatus(protocolConfiguration.getReferenceOrThrow(), ConnectionStatus.ERROR);
            return;
        }

        if (!protocolConfiguration.isEnabled()) {
            updateStatus(protocolConfiguration.getReferenceOrThrow(), ConnectionStatus.DISABLED);
            return;
        }
        
        AttributeRef protocolRef = protocolConfiguration.getReferenceOrThrow();

        synchronized (knxConnections) {
            Consumer<ConnectionStatus> statusConsumer = status -> {
                updateStatus(protocolRef, status);
            };

            KNXConnection knxConnection = knxConnections.computeIfAbsent(
                            gatewayIpParam.get(), gatewayIp ->
                    new KNXConnection(gatewayIp, executorService)
            );
            knxConnection.addConnectionStatusConsumer(statusConsumer);
            knxConnection.connect();

            synchronized (statusConsumerMap) {
                statusConsumerMap.put(protocolRef, statusConsumer);
            }
        }
    }

    @Override
    protected void doUnlinkProtocolConfiguration(AssetAttribute protocolConfiguration) {

        Consumer<ConnectionStatus> statusConsumer;
        synchronized (statusConsumerMap) {
            statusConsumer = statusConsumerMap.get(protocolConfiguration.getReferenceOrThrow());
        }

        String gatewayIp = protocolConfiguration.getMetaItem(KNX_GATEWAY_IP).flatMap(AbstractValueHolder::getValueAsString).orElse("");
        synchronized (knxConnections) {
            KNXConnection knxConnection = knxConnections.get(gatewayIp);
            if (knxConnection != null) {
                if (!isKNXConnectionStillUsed(knxConnection)) {
                    knxConnection.removeConnectionStatusConsumer(statusConsumer);
                    knxConnection.disconnect();
                    knxConnections.remove(gatewayIp);
                }
            }
        }
    }

    @Override
    protected void doLinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
        String gatewayIp = protocolConfiguration.getMetaItem(KNX_GATEWAY_IP).flatMap(AbstractValueHolder::getValueAsString).orElse("");
        final AttributeRef attributeRef = attribute.getReferenceOrThrow();

        // Check there is a DPT
        Optional<String> dpt = attribute.getMetaItem(DPT).flatMap(AbstractValueHolder::getValueAsString);
        if (!dpt.isPresent()) {
            LOG.severe("No DPT for protocol attribute: " + attributeRef);
            return;
        }

        Optional<String> statusGA = attribute.getMetaItem(STATUS_GA).flatMap(AbstractValueHolder::getValueAsString);
        Optional<String> actionGA = attribute.getMetaItem(ACTION_GA).flatMap(AbstractValueHolder::getValueAsString);

        if (!statusGA.isPresent() && !actionGA.isPresent()) {
            LOG.warning("No status group address or action group address provided so nothing to do for protocol attribute: " + attributeRef);
            return;
        }

        KNXConnection knxConnection = getConnection(gatewayIp);

        if (knxConnection == null) {
            // Means that the protocol configuration is disabled
            return;
        }

        // If this attribute relates to a read groupthen start monitoring that measurement and broadcast any changes to the value
        statusGA.ifPresent(groupAddress -> {
            try {
                addStatusDatapoint(attributeRef, knxConnection, groupAddress, dpt.get());
            } catch (KNXFormatException e) {
                LOG.severe("Give action group address is invalid for protocol attribute: " + attributeRef + " - " + e.getMessage());
            }
        });

        // If this attribute relates to an action then store it
        actionGA.ifPresent(groupAddress -> {
            try {
                addActionDatapoint(attributeRef, knxConnection, groupAddress, dpt.get());
            } catch (KNXFormatException e) {
                LOG.severe("Give action group address is invalid for protocol attribute: " + attributeRef + " - " + e.getMessage());
            }
        });
    }


    @Override
    protected void doUnlinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
        final AttributeRef attributeRef = attribute.getReferenceOrThrow();

        // If this attribute is registered for status updates then un-subscribe it
        removeStatusDatapoint(attributeRef);

        // If this attribute has a stored action then remove it
        removeActionDatapoint(attributeRef);
    }

    @Override
    protected void processLinkedAttributeWrite(AttributeEvent event, AssetAttribute protocolConfiguration) {
        if (!protocolConfiguration.isEnabled()) {
            LOG.fine("Protocol configuration is disabled so ignoring write request");
            return;
        }

        synchronized (attributeActionMap) {
            Pair<KNXConnection, Datapoint> controlInfo = attributeActionMap.get(event.getAttributeRef());

            if (controlInfo == null) {
                LOG.fine("Attribute isn't linked to a KNX datapoint so cannot process write: " + event);
                return;
            }

            LOG.fine("Sending to KNX datapoint '" + controlInfo.value + "': " + event.getValue());
            controlInfo.key.sendCommand(controlInfo.value, event.getValue());

            // We assume KNX actuator will send new status on relevant status group address which will be picked up by listener and updates the state again later
            updateLinkedAttribute(event.getAttributeState());
        }
    }

    protected KNXConnection getConnection(String gatewayIp) {
        synchronized (knxConnections) {
            return knxConnections.get(gatewayIp);
        }
    }
    
    protected void addActionDatapoint(AttributeRef attributeRef, KNXConnection knxConnection, String groupAddress, String dpt) throws KNXFormatException {
        synchronized (attributeActionMap) {
            Datapoint datapoint = new CommandDP(new GroupAddress(groupAddress), attributeRef.getAttributeName());
            datapoint.setDPT(0, dpt);
            Pair<KNXConnection, Datapoint> controlInfo = attributeActionMap.get(attributeRef);
            if (controlInfo != null) {
                return;
            }

            attributeActionMap.put(attributeRef, new Pair<>(knxConnection, datapoint));
            LOG.info("Attribute registered for sending commands: " + attributeRef);
        }
    }
    
    protected void removeActionDatapoint(AttributeRef attributeRef) {
        synchronized (attributeActionMap) {
            attributeActionMap.remove(attributeRef);
        }
    }

    protected void addStatusDatapoint(AttributeRef attributeRef, KNXConnection knxConnection, String groupAddress, String dpt) throws KNXFormatException {
        synchronized (attributeStatusMap) {
            Datapoint datapoint = new StateDP(new GroupAddress(groupAddress), attributeRef.getAttributeName());
            datapoint.setDPT(0, dpt);
            Pair<KNXConnection, Datapoint> controlInfo = attributeStatusMap.get(attributeRef);
            if (controlInfo != null) {
                return;
            }
            
            //TODO inform listener on KNXConnection to update this attribute when changes are coming in. then call updateLinkedAttribute()
            //updateLinkedAttribute(AttributeState state, long timestamp) when receiving event from KNX bus
            
            attributeStatusMap.put(attributeRef, new Pair<>(knxConnection, datapoint));
            LOG.info("Attribute registered for sending commands: " + attributeRef);
        }
    }
    
    protected void removeStatusDatapoint(AttributeRef attributeRef) {
        //TODO remove from listener on KNXConnection
        synchronized (attributeStatusMap) {
            attributeStatusMap.remove(attributeRef);
        }
    }
    
    
    public Map<AttributeRef, Pair<KNXConnection, Datapoint>> getAttributeActionMap() {
        return attributeActionMap;
    }

    
    public Map<AttributeRef, Pair<KNXConnection, Datapoint>> getAttributeStatusMap() {
        return attributeStatusMap;
    }
    
    
    protected boolean isKNXConnectionStillUsed(KNXConnection knxConnection) {
        boolean clientStillUsed;

        synchronized (attributeStatusMap) {
            clientStillUsed = attributeStatusMap.values().stream().anyMatch(p -> p.key == knxConnection);
        }

        if (!clientStillUsed) {
            synchronized (attributeActionMap) {
                clientStillUsed = attributeActionMap.values().stream().anyMatch(p -> p.key == knxConnection);
            }
        }

        return clientStillUsed;
    }
 
}
