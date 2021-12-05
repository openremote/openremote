package org.openremote.agent.protocol.bluetooth.mesh.transport;

/**
 * Abstract wrapper class for mesh message.
 */
public abstract class MeshMessage {

    private final int mAszmic = 0;
    protected Message mMessage;
    byte[] mParameters;
    protected Integer messageTtl = null;

    /**
     * Returns the application key flag used for this message.
     *
     * @return application key flag
     */
    abstract int getAkf();

    /**
     * Returns application key identifier used for this message.
     *
     * @return application key identifier
     */
    abstract int getAid();

    /**
     * Returns the opCode of this message
     *
     * @return opcode
     */
    public abstract int getOpCode();

    /**
     * Returns the parameters of this message.
     *
     * @return parameters
     */
    abstract byte[] getParameters();

    /**
     * Returns the size of message integrity check used for this message.
     *
     * @return aszmic
     */
    public final int getAszmic() {
        return mAszmic;
    }

    /**
     * Returns the message
     */
    public Message getMessage() {
        return mMessage;
    }

    /**
     * Set the access message
     *
     * @param message access message
     */
    void setMessage(final Message message) {
        mMessage = message;
    }

    /**
     * Returns the source address of the message
     */
    public int getSrc() {
        return mMessage.getSrc();
    }

    /**
     * Returns the destination address of the message
     */
    public int getDst() {
        return mMessage.getDst();
    }

    /**
     * Returns the TTL set for the mesh message
     * @return TTL value or null if not set.
     */
    public Integer getMessageTtl() {
        return messageTtl;
    }

    /**
     * Sets the TTL for this message.
     * If a TTL is not specified the message will use the default ttl set for the provisioner node.
     * @param messageTtl TTL value
     */
    public void setMessageTtl(final Integer messageTtl) {
        this.messageTtl = messageTtl;
    }
}

