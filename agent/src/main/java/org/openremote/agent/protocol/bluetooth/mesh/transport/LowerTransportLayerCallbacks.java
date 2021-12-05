package org.openremote.agent.protocol.bluetooth.mesh.transport;

public interface LowerTransportLayerCallbacks {

    /**
     * Callback to notify that a block acknowledgement message be sent now.
     *
     * @param controlMessage control message to be sent
     */
    void sendSegmentAcknowledgementMessage(final ControlMessage controlMessage);

    /**
     * Callback to notify that the incomplete timer has expired.
     */
    void onIncompleteTimerExpired();

    int getTtl();
}
