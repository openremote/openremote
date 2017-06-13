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

public class KNXProtocol extends AbstractProtocol {

    private static final Logger LOG = Logger.getLogger(KNXProtocol.class.getName());

    public static final String PROTOCOL_NAME = PROTOCOL_NAMESPACE + ":knx";
    public static final String KNX_GATEWAY_IP = PROTOCOL_NAME + ":gatewayIp";

    final protected Map<String, KNXConnection> knxConnections = new HashMap<>();
    final protected Map<AttributeRef, Consumer<ConnectionStatus>> statusConsumerMap = new HashMap<>();
    
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
                knxConnection.removeConnectionStatusConsumer(statusConsumer);

                //TODO check if KNXConnection is still used
                knxConnections.remove(gatewayIp);
                knxConnection.disconnect();
            }
        }
    }

    @Override
    protected void doLinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
        // TODO different attributes can have same group address
        //create calimero StateDP ga, name, read address, write address

    }

    @Override
    protected void doUnlinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
        // TODO 

    }

    @Override
    protected void processLinkedAttributeWrite(AttributeEvent event, AssetAttribute protocolConfiguration) {
        // TODO  groupWrite   - event.getAttributeRef() to identify groupaddress   
        // event.getValue() - new value to send to knx
        
    }

    protected KNXConnection getConnection(String gatewayIp) {
        synchronized (knxConnections) {
            return knxConnections.get(gatewayIp);
        }
    }
    
    //TODO call updateLinkedAttribute(AttributeState state, long timestamp) when receiving event from KNX bus
}
