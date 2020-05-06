package org.openremote.agent.protocol.tradfri.device.event;

import org.openremote.agent.protocol.tradfri.device.Remote;

/**
 * The class that represents a remote event that occurred to an IKEA TRÅDFRI remote
 * @author Stijn Groenen
 * @version 1.0.0
 */
public class RemoteEvent extends DeviceEvent {

    /**
     * Construct the RemoteEvent class
     * @param remote The remote for which the event occurred
     * @since 1.0.0
     */
    public RemoteEvent(Remote remote) {
        super(remote);
    }

    /**
     * Get the remote for which the event occurred
     * @return The remote for which the event occurred
     * @since 1.0.0
     */
    public Remote getRemote(){
        return (Remote) getDevice();
    }

}
