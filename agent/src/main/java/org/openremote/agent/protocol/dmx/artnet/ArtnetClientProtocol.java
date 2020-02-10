package org.openremote.agent.protocol.dmx.artnet;

import org.openremote.agent.protocol.dmx.AbstractDMXClientProtocol;
import org.openremote.agent.protocol.dmx.AbstractDMXLightState;
import org.openremote.agent.protocol.io.IoClient;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.MetaItemDescriptor;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

public class ArtnetClientProtocol extends AbstractDMXClientProtocol {

    @Override
    public Map<Integer, AbstractDMXLightState> getLightStateMemory() {
        return null;
    }

    @Override
    protected IoClient<String> createIoClient(String host, int port, Integer bindPort, Charset charset, boolean binaryMode, boolean hexMode) {
        return null;
    }

    @Override
    protected List<MetaItemDescriptor> getLinkedAttributeMetaItemDescriptors() {
        return null;
    }

    @Override
    protected void doLinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) throws IOException {

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
