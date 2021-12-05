package org.openremote.agent.protocol.bluetooth.mesh.transport;

import org.openremote.agent.protocol.bluetooth.mesh.opcodes.ConfigMessageOpCodes;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;

/**
 * Creates the ConfigDefaultTtlSet message.
 */
public class ConfigDefaultTtlStatus extends ConfigStatusMessage {

    private static final String TAG = ConfigDefaultTtlStatus.class.getSimpleName();
    private static final int OP_CODE = ConfigMessageOpCodes.CONFIG_DEFAULT_TTL_STATUS;

    private int mTtl;

    /**
     * Constructs ConfigDefaultTtlStatus message.
     *
     * @param message {@link AccessMessage}
     */
    public ConfigDefaultTtlStatus(final AccessMessage message) {
        super(message);
        mParameters = message.getParameters();
        parseStatusParameters();
    }

    @Override
    void parseStatusParameters() {
        mTtl = MeshParserUtils.unsignedByteToInt(mParameters[0]);
    }

    @Override
    public int getOpCode() {
        return OP_CODE;
    }

    /**
     * Returns the ttl value
     */
    public int getTtl() {
        return mTtl;
    }
}

