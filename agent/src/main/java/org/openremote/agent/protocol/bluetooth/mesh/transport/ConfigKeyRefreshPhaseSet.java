package org.openremote.agent.protocol.bluetooth.mesh.transport;

import org.openremote.agent.protocol.bluetooth.mesh.NetworkKey;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;

import java.util.logging.Logger;

import static org.openremote.agent.protocol.bluetooth.mesh.opcodes.ConfigMessageOpCodes.CONFIG_KEY_REFRESH_PHASE_SET;

/**
 * Creates the ConfigKeyRefreshPhaseSet message.
 */
public class ConfigKeyRefreshPhaseSet extends ConfigMessage {

    public static final Logger LOG = Logger.getLogger(ConfigKeyRefreshPhaseSet.class.getName());
    private static final int OP_CODE = CONFIG_KEY_REFRESH_PHASE_SET;
    private final NetworkKey mNetKey;
    private final /* @KeyRefreshPhaseTransition */
    int transition;

    /**
     * Constructs ConfigKeyRefreshPhaseSet message.
     *
     * @param networkKey {@link NetworkKey}
     */
    public ConfigKeyRefreshPhaseSet(final NetworkKey networkKey, /* @KeyRefreshPhaseTransition */ final int transition) {
        mNetKey = networkKey;
        this.transition = transition;
        assembleMessageParameters();
    }

    @Override
    public int getOpCode() {
        return OP_CODE;
    }

    @Override
    void assembleMessageParameters() {
        LOG.info("NetKeyIndex: " + mNetKey.getKeyIndex());
        final byte[] netKeyIndex = MeshParserUtils.addKeyIndexPadding(mNetKey.getKeyIndex());
        mParameters = new byte[]{netKeyIndex[1], (byte) ((netKeyIndex[0] & 0xFF) & 0x0F), (byte) transition};
    }
}

