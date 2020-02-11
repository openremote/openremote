package org.openremote.agent.protocol.dmx;

import org.openremote.agent.protocol.udp.AbstractUdpClientProtocol;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractDMXClientProtocol extends AbstractUdpClientProtocol<String> {

    public abstract Map<Integer, AbstractDMXLightState> getLightStateMemory();

    public abstract void updateLightInMemory(Integer lightId, AbstractDMXLightState updatedLightState);

}
