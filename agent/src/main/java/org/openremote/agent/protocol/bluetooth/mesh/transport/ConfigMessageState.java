package org.openremote.agent.protocol.bluetooth.mesh.transport;

/**
 * ConfigMessageState class that handles configuration message state.
 */
class ConfigMessageState extends MeshMessageState {

    private final byte[] mDeviceKey;

    /**
     * Constructs the ConfigMessageState
     *
     * @param src           Source address
     * @param dst           Destination address
     * @param deviceKey     Device key
     * @param meshMessage   {@link MeshMessage} Mesh message to be sent
     * @param meshTransport {@link MeshTransport} Mesh transport
     * @param callbacks     {@link InternalMeshMsgHandlerCallbacks} callbacks
     */
    ConfigMessageState(final int src,
                       final int dst,
                       final byte[] deviceKey,
                       final MeshMessage meshMessage,
                       final MeshTransport meshTransport,
                       final InternalMeshMsgHandlerCallbacks callbacks) {
        super(meshMessage, meshTransport, callbacks);
        this.mSrc = src;
        this.mDst = dst;
        this.mDeviceKey = deviceKey;
        createAccessMessage();
    }

    @Override
    public synchronized MessageState getState() {
        return MessageState.CONFIG_MESSAGE_STATE;
    }

    private synchronized void createAccessMessage() throws IllegalArgumentException {
        final ConfigMessage configMessage = (ConfigMessage) mMeshMessage;
        final int akf = configMessage.getAkf();
        final int aid = configMessage.getAid();
        final int aszmic = configMessage.getAszmic();
        final int opCode = configMessage.getOpCode();
        final byte[] parameters = configMessage.getParameters();
        message = mMeshTransport.createMeshMessage(mSrc, mDst, configMessage.messageTtl,
            mDeviceKey, akf, aid, aszmic, opCode, parameters);
        configMessage.setMessage(message);
    }
}

