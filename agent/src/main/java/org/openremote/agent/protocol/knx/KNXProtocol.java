package org.openremote.agent.protocol.knx;

import static org.openremote.model.Constants.PROTOCOL_NAMESPACE;

import java.util.Optional;
import java.util.logging.Logger;

import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.agent.protocol.ConnectionStatus;
import org.openremote.model.AbstractValueHolder;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeState;

public class KNXProtocol extends AbstractProtocol {

    private static final Logger LOG = Logger.getLogger(KNXProtocol.class.getName());


    public static final String PROTOCOL_NAME = PROTOCOL_NAMESPACE + ":knx";
    public static final String KNX_GATEWAY_IP = PROTOCOL_NAME + ":gatewayIp";
    
 
    @Override
    public String getProtocolName() {
        return PROTOCOL_NAME;
    }

    @Override
    protected void doLinkProtocolConfiguration(AssetAttribute protocolConfiguration) {
        Optional<String> gatewayIp = protocolConfiguration.getMetaItem(KNX_GATEWAY_IP).flatMap(AbstractValueHolder::getValueAsString);
        if (!gatewayIp.isPresent()) {
            LOG.severe("No KNX gateway IP address provided for protocol configuration: " + protocolConfiguration);
            updateStatus(protocolConfiguration.getReferenceOrThrow(), ConnectionStatus.ERROR);
            return;
        }

        if (!protocolConfiguration.isEnabled()) {
            updateStatus(protocolConfiguration.getReferenceOrThrow(), ConnectionStatus.DISABLED);
            return;
        }

        updateStatus(protocolConfiguration.getReferenceOrThrow(), ConnectionStatus.CONNECTED);
        //TODO Create KNXConnection for given IP and config. KNXConnection will be main class for KNX communication and will be the listener for bus events
    }

    @Override
    protected void doUnlinkProtocolConfiguration(AssetAttribute protocolConfiguration) {
        // TODO retrieve related KNX connection and close link to KNX bus

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

    
    
    //TODO call updateLinkedAttribute(AttributeState state, long timestamp) when receiving event from KNX bus
}
