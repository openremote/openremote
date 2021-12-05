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
public final class LightLightnessStatus extends GenericStatusMessage {

    public static final Logger LOG = Logger.getLogger(LightLightnessStatus.class.getName());
    private static final int LIGHT_LIGHTNESS_STATUS_MANDATORY_LENGTH = 2;
    private static final int OP_CODE = ApplicationMessageOpCodes.LIGHT_LIGHTNESS_STATUS;
    private int mPresentLightness;
    private Integer mTargetLightness;
    private int mTransitionSteps;
    private int mTransitionResolution;

    /**
     * Constructs LightLightnessStatus message
     *
     * @param message access message
     */
    public LightLightnessStatus(final AccessMessage message) {
        super(message);
        this.mMessage = message;
        this.mParameters = message.getParameters();
        parseStatusParameters();
    }

    @Override
    void parseStatusParameters() {
        LOG.info("Received light lightness status from: " + MeshAddress.formatAddress(mMessage.getSrc(), true));
        final ByteBuffer buffer = ByteBuffer.wrap(mParameters).order(ByteOrder.LITTLE_ENDIAN);
        mPresentLightness = buffer.getShort() & 0xFFFF;
        LOG.info("Present level: " + mPresentLightness);
        if (buffer.limit() > LIGHT_LIGHTNESS_STATUS_MANDATORY_LENGTH) {
            mTargetLightness = buffer.getShort() & 0xFFFF;
            final int remainingTime = buffer.get() & 0xFF;
            mTransitionSteps = (remainingTime & 0x3F);
            mTransitionResolution = (remainingTime >> 6);
            LOG.info("Target level: " + mTargetLightness);
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
    public final int getPresentLightness() {
        return mPresentLightness;
    }

    /**
     * Returns the target level of the GenericOnOffModel
     *
     * @return target level
     */
    public final Integer getTargetLightness() {
        return mTargetLightness;
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

