package org.openremote.agent.protocol.bluetooth.mesh.transport;

import org.openremote.agent.protocol.bluetooth.mesh.ApplicationKey;

abstract class GenericMessage extends MeshMessage {

    public static final int GENERIC_TRANSITION_STEP_0 = 0;
    public static final int GENERIC_TRANSITION_STEP_1 = 1;
    public static final int GENERIC_TRANSITION_STEP_2 = 2;
    public static final int GENERIC_TRANSITION_STEP_3 = 3;

    final ApplicationKey mAppKey;
    byte mAid;

    /**
     * Constructs a generic message
     *
     * @param appKey application key
     */
    GenericMessage(final ApplicationKey appKey) {
        if (appKey.getKey().length != 16)
            throw new IllegalArgumentException("Application key must be 16 bytes");
        this.mAppKey = appKey;
    }

    @Override
    public final int getAkf() {
        return 1;
    }

    @Override
    public final int getAid() {
        return mAid;
    }

    /**
     * Returns the app key used in this message.
     *
     * @return app key
     */
    public final ApplicationKey getAppKey() {
        return mAppKey;
    }

    @Override
    public final byte[] getParameters() {
        return mParameters;
    }

    /**
     * Creates the parameters for a given mesh message.
     */
    abstract void assembleMessageParameters();
}

