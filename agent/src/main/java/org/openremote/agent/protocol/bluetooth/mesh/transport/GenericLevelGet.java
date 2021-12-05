package org.openremote.agent.protocol.bluetooth.mesh.transport;

import org.openremote.agent.protocol.bluetooth.mesh.ApplicationKey;
import org.openremote.agent.protocol.bluetooth.mesh.opcodes.ApplicationMessageOpCodes;
import org.openremote.agent.protocol.bluetooth.mesh.utils.SecureUtils;

import java.util.logging.Logger;

/**
 * To be used as a wrapper class to create generic level get message.
 */
public class GenericLevelGet extends GenericMessage {

    public static final Logger LOG = Logger.getLogger(GenericLevelGet.class.getName());
    private static final int OP_CODE = ApplicationMessageOpCodes.GENERIC_LEVEL_GET;


    /**
     * Constructs GenericLevelGet message.
     *
     * @param appKey {@link ApplicationKey} key for this message
     * @throws IllegalArgumentException if any illegal arguments are passed
     */
    public GenericLevelGet(final ApplicationKey appKey) throws IllegalArgumentException {
        super(appKey);
        assembleMessageParameters();
    }

    @Override
    public int getOpCode() {
        return OP_CODE;
    }

    @Override
    void assembleMessageParameters() {
        mAid = SecureUtils.calculateK4(mAppKey.getKey());
    }
}
