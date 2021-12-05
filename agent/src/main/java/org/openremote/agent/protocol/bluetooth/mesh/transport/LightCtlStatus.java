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
public final class LightCtlStatus extends GenericStatusMessage {

    public static final Logger LOG = Logger.getLogger(LightCtlStatus.class.getName());
    private static final int LIGHT_CTL_STATUS_MANDATORY_LENGTH = 4;
    private static final int OP_CODE = ApplicationMessageOpCodes.LIGHT_CTL_STATUS;
    private int mPresentCtlLightness;
    private int mPresentCtlTemperature;
    private Integer mTargetCtlLightness;
    private Integer mTargetCtlTemperature;
    private int mTransitionSteps;
    private int mTransitionResolution;

    /**
     * Constructs LightCtlStatus message
     *
     * @param message access message
     */
    public LightCtlStatus(final AccessMessage message) {
        super(message);
        this.mMessage = message;
        this.mParameters = message.getParameters();
        parseStatusParameters();
    }

    @Override
    void parseStatusParameters() {
        LOG.info("Received light ctl status from: " + MeshAddress.formatAddress(mMessage.getSrc(), true));
        final ByteBuffer buffer = ByteBuffer.wrap(mParameters).order(ByteOrder.LITTLE_ENDIAN);
        mPresentCtlLightness = buffer.getShort() & 0xFFFF;
        mPresentCtlTemperature = buffer.getShort() & 0xFFFF;
        LOG.info("Present lightness: " + mPresentCtlLightness);
        LOG.info("Present temperature: " + mPresentCtlTemperature);
        if (buffer.limit() > LIGHT_CTL_STATUS_MANDATORY_LENGTH) {
            mTargetCtlLightness = buffer.getShort() & 0xFFFF;
            mTargetCtlTemperature = buffer.getShort() & 0xFFFF;
            final int remainingTime = buffer.get() & 0xFF;
            mTransitionSteps = (remainingTime & 0x3F);
            mTransitionResolution = (remainingTime >> 6);
            LOG.info("Target lightness: " + mTargetCtlLightness);
            LOG.info("Target temperature: " + mTargetCtlTemperature);
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
        return mPresentCtlLightness;
    }

    /**
     * Returns the target level of the GenericOnOffModel
     *
     * @return target level
     */
    public final Integer getTargetLightness() {
        return mTargetCtlLightness;
    }

    /**
     * Returns the present level of the GenericOnOffModel
     *
     * @return present level
     */
    public final int getPresentTemperature() {
        return mPresentCtlTemperature;
    }

    /**
     * Returns the target level of the GenericOnOffModel
     *
     * @return target level
     */
    public final Integer getTargetTemperature() {
        return mTargetCtlTemperature;
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

