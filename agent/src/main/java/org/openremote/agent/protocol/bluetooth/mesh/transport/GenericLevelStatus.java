package org.openremote.agent.protocol.bluetooth.mesh.transport;

import org.openremote.agent.protocol.bluetooth.mesh.opcodes.ApplicationMessageOpCodes;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshAddress;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Logger;

/**
 * To be used as a wrapper class to create generic level status message.
 */
public final class GenericLevelStatus extends GenericStatusMessage {

    public static final Logger LOG = Logger.getLogger(GenericLevelStatus.class.getName());
    private static final int GENERIC_LEVEL_STATUS_MANDATORY_LENGTH = 2;
    private static final int OP_CODE = ApplicationMessageOpCodes.GENERIC_LEVEL_STATUS;
    private int mPresentLevel;
    private Integer mTargetLevel;
    private int mTransitionSteps;
    private int mTransitionResolution;

    /**
     * Constructs GenericLevelStatus message
     * @param message access message
     */
    public GenericLevelStatus(final AccessMessage message) {
        super(message);
        this.mMessage = message;
        this.mParameters = message.getParameters();
        parseStatusParameters();
    }

    @Override
    void parseStatusParameters() {
        LOG.info("Received generic level status from: " + MeshAddress.formatAddress(mMessage.getSrc(), true));
        final ByteBuffer buffer = ByteBuffer.wrap(mParameters).order(ByteOrder.LITTLE_ENDIAN);
        mPresentLevel = (int) (buffer.getShort());
        LOG.info("Present level: " + mPresentLevel);
        if (buffer.limit() > GENERIC_LEVEL_STATUS_MANDATORY_LENGTH) {
            mTargetLevel = (int) (buffer.getShort());
            final int remainingTime = buffer.get() & 0xFF;
            mTransitionSteps = (remainingTime & 0x3F);
            mTransitionResolution = (remainingTime >> 6);
            LOG.info("Target level: " + mTargetLevel);
            LOG.info("Remaining time, transition number of steps: " + mTransitionSteps);
            LOG.info("Remaining time, transition number of step resolution: " + mTransitionResolution);
            LOG.info("Remaining time: " + MeshParserUtils.getRemainingTime(remainingTime));
        }
    }

    @Override
    public int getOpCode() {
        return OP_CODE;
    }

    /**
     * Returns the present level of the GenericOnOffModel
     *
     * @return present level
     */
    public final int getPresentLevel() {
        return mPresentLevel;
    }

    /**
     * Returns the target level of the GenericOnOffModel
     *
     * @return target level
     */
    public final Integer getTargetLevel() {
        return mTargetLevel;
    }

    /**
     * Returns the transition steps.
     *
     * @return transition steps
     */
    public int getTransitionSteps() {
        return mTransitionSteps;
    }

    /**
     * Returns the transition resolution.
     *
     * @return transition resolution
     */
    public int getTransitionResolution() {
        return mTransitionResolution;
    }
}

