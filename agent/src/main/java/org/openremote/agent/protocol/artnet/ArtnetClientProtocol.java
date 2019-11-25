package org.openremote.agent.protocol.artnet;

import org.openremote.agent.protocol.io.IoClient;
import org.openremote.agent.protocol.udp.AbstractUdpClientProtocol;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.MetaItemDescriptor;

import java.nio.charset.Charset;
import java.util.List;

public class ArtnetClientProtocol extends AbstractUdpClientProtocol {

    @Override
    protected IoClient createIoClient(String host, int port, Integer bindPort, Charset charset, boolean binaryMode, boolean hexMode) {
        return null;
    }

    @Override
    protected List<MetaItemDescriptor> getLinkedAttributeMetaItemDescriptors() {
        return null;
    }

    @Override
    protected void doLinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {

    }

    @Override
    protected void doUnlinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {

    }

    @Override
    protected void processLinkedAttributeWrite(AttributeEvent event, AssetAttribute protocolConfiguration) {

    }

    @Override
    public String getProtocolName() {
        return null;
    }

    @Override
    public String getProtocolDisplayName() {
        return null;
    }

    @Override
    public String getVersion() {
        return null;
    }
}
