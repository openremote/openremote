package org.openremote.agent.protocol.bluetooth.mesh;

import org.openremote.agent.protocol.bluetooth.mesh.transport.BaseMeshMessageHandler;
import org.openremote.agent.protocol.bluetooth.mesh.transport.NetworkLayerCallbacks;
import org.openremote.agent.protocol.bluetooth.mesh.transport.UpperTransportLayerCallbacks;
import org.openremote.agent.protocol.bluetooth.mesh.utils.ExtendedInvalidCipherTextException;

/**
 * MeshMessageHandler class for handling mesh
 */
final class MeshMessageHandler extends BaseMeshMessageHandler {

    /**
     * Constructs MeshMessageHandler
     *
     * @param internalTransportCallbacks   {@link InternalTransportCallbacks} Callbacks
     * @param networkLayerCallbacks        {@link NetworkLayerCallbacks} network layer callbacks
     * @param upperTransportLayerCallbacks {@link UpperTransportLayerCallbacks} upper transport layer callbacks
     */
    MeshMessageHandler(final InternalTransportCallbacks internalTransportCallbacks,
                       final NetworkLayerCallbacks networkLayerCallbacks,
                       final UpperTransportLayerCallbacks upperTransportLayerCallbacks) {
        super(internalTransportCallbacks, networkLayerCallbacks, upperTransportLayerCallbacks);
    }

    @Override
    protected synchronized final void setMeshStatusCallbacks(final MeshStatusCallbacks statusCallbacks) {
        mStatusCallbacks = statusCallbacks;
    }


    @Override
    protected synchronized final void parseMeshPduNotifications(final byte[] pdu, final MeshNetwork network) throws ExtendedInvalidCipherTextException {
        super.parseMeshPduNotifications(pdu, network);
    }
}

