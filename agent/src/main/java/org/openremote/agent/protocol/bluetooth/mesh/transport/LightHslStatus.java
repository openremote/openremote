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
@SuppressWarnings({"WeakerAccess"})
public final class LightHslStatus extends GenericStatusMessage {

    public static final Logger LOG = Logger.getLogger(LightHslStatus.class.getName());
    private static final int LIGHT_CTL_STATUS_MANDATORY_LENGTH = 6;
    private static final int OP_CODE = ApplicationMessageOpCodes.LIGHT_HSL_STATUS;
    private int mPresentHslLightness;
    private int mPresentHslHue;
    private int mPresentHslSaturation;
    private int mTransitionSteps;
    private int mTransitionResolution;

    /**
     * Constructs LightHslStatus message
     * @param message access message
     */
    public LightHslStatus(final AccessMessage message) {
        super(message);
        this.mMessage = message;
        this.mParameters = message.getParameters();
        parseStatusParameters();
    }

    @Override
    void parseStatusParameters() {
        LOG.info("Received light hsl status from: " + MeshAddress.formatAddress(mMessage.getSrc(), true));
        final ByteBuffer buffer = ByteBuffer.wrap(mParameters).order(ByteOrder.LITTLE_ENDIAN);
        mPresentHslLightness = buffer.getShort() & 0xFFFF;
        mPresentHslHue = buffer.getShort() & 0xFFFF;
        mPresentHslSaturation = buffer.getShort() & 0xFFFF;
        LOG.info("Present lightness: " + mPresentHslLightness);
        LOG.info("Present hue: " + mPresentHslHue);
        LOG.info("Present saturation: " + mPresentHslSaturation);
        if (buffer.limit() > LIGHT_CTL_STATUS_MANDATORY_LENGTH) {
            final int remainingTime = buffer.get() & 0xFF;
            mTransitionSteps = (remainingTime & 0x3F);
            mTransitionResolution = (remainingTime >> 6);
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
        return mPresentHslLightness;
    }

    /**
     * Returns the present level of the GenericOnOffModel
     *
     * @return present level
     */
    public final int getPresentSaturation() {
        return mPresentHslSaturation;
    }

    /**
     * Returns the present level of the GenericOnOffModel
     *
     * @return present level
     */
    public final int getPresentHue() {
        return mPresentHslHue;
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

