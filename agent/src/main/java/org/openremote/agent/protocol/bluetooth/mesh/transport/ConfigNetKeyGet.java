package org.openremote.agent.protocol.bluetooth.mesh.transport;

import org.openremote.agent.protocol.bluetooth.mesh.opcodes.ConfigMessageOpCodes;

public class ConfigNetKeyGet extends ConfigMessage {

    private static final String TAG = ConfigNetKeyGet.class.getSimpleName();
    private static final int OP_CODE = ConfigMessageOpCodes.CONFIG_NETKEY_GET;

    /**
     * Constructs ConfigNetKeyGet message.
     */
    public ConfigNetKeyGet() {
        assembleMessageParameters();
    }

    @Override
    public int getOpCode() {
        return OP_CODE;
    }

    @Override
    void assembleMessageParameters() {
        //Do nothing as ConfigNetKeyGet message does not have parameters
    }
}
