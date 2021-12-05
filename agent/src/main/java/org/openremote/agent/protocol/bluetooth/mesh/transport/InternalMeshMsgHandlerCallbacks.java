package org.openremote.agent.protocol.bluetooth.mesh.transport;

/**
 * Callbacks to notify the mesh message handler to notify events from transport layers.
 */
public interface InternalMeshMsgHandlerCallbacks {
    /**
     * Callback to notify the incomplete timer has expired
     *
     * @param address address of the message
     */
    void onIncompleteTimerExpired(final int address);
}
