package org.openremote.agent.protocol.bluetooth.mesh.transport;

import java.util.UUID;

/**
 * Mesh message handler api
 */
public interface MeshMessageHandlerApi {
    /**
     * Sends a mesh message specified within the {@link MeshMessage} object
     *
     * @param label       Label UUID for destination address
     * @param meshMessage {@link MeshMessage} Mesh message containing the message opcode and message parameters
     */
    void createMeshMessage(final int src, final int dst, /* @Nullable */ final UUID label, final MeshMessage meshMessage);
}
