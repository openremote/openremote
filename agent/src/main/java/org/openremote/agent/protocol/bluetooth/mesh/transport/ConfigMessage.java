package org.openremote.agent.protocol.bluetooth.mesh.transport;

abstract class ConfigMessage extends MeshMessage {

    /**
     * Creates the parameters for a given mesh message.
     */
    abstract void assembleMessageParameters();

    @Override
    public final int getAkf() {
        return 0;
    }

    @Override
    public final int getAid() {
        return 0;
    }

    @Override
    public final byte[] getParameters() {
        return mParameters;
    }
}
