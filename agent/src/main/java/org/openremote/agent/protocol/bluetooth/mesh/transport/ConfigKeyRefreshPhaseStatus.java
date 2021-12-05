package org.openremote.agent.protocol.bluetooth.mesh.transport;

import java.util.ArrayList;
import java.util.logging.Logger;

// import static org.openremote.agent.protocol.bluetooth.mesh.NetworkKey.KeyRefreshPhase;
// import static org.openremote.agent.protocol.bluetooth.mesh.NetworkKey.KeyRefreshPhaseTransition;
import static org.openremote.agent.protocol.bluetooth.mesh.opcodes.ConfigMessageOpCodes.CONFIG_KEY_REFRESH_PHASE_STATUS;


/**
 * To be used as a wrapper class for when creating the ConfigAppKeyStatus Message.
 */
public class ConfigKeyRefreshPhaseStatus extends ConfigStatusMessage {

    public static final Logger LOG = Logger.getLogger(ConfigKeyRefreshPhaseStatus.class.getName());
    private static final int OP_CODE = CONFIG_KEY_REFRESH_PHASE_STATUS;
    private int mNetKeyIndex;
    private /* @KeyRefreshPhase */ int transition;

    /**
     * Constructs the ConfigAppKeyStatus mMessage.
     *
     * @param message Access Message
     */
    public ConfigKeyRefreshPhaseStatus(final AccessMessage message) {
        super(message);
        this.mParameters = message.getParameters();
        parseStatusParameters();
    }

    @Override
    final void parseStatusParameters() {
        mStatusCode = mParameters[0];
        mStatusCodeName = getStatusCodeName(mStatusCode);

        final ArrayList<Integer> keyIndexes = decode(mParameters.length, 1);
        mNetKeyIndex = keyIndexes.get(0);
        transition = mParameters[3];

        LOG.info("Status code: " + mStatusCode);
        LOG.info("Status message: " + mStatusCodeName);
        LOG.info("Net key index: " + Integer.toHexString(mNetKeyIndex));
        LOG.info("Transition: " + transition);
    }

    @Override
    public final int getOpCode() {
        return OP_CODE;
    }

    /**
     * Returns the global index of the net key.
     *
     * @return netkey index
     */
    public final int getNetKeyIndex() {
        return mNetKeyIndex;
    }

    /**
     * Returns the current key refresh phase.
     */
    /* @KeyRefreshPhaseTransition */
    public final int getTransition() {
        return transition;
    }

    /**
     * Returns if the message was successful
     *
     * @return true if the message was successful or false otherwise
     */
    public final boolean isSuccessful() {
        return mStatusCode == 0x00;
    }
}

