/*
 * Copyright 2021, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.agent.protocol.bluetooth.mesh.transport;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Logger;

import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshAddress;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;

import static org.openremote.agent.protocol.bluetooth.mesh.opcodes.ApplicationMessageOpCodes.SCENE_STATUS;

/**
 * To be used as a wrapper class for when creating the GenericOnOffStatus Message.
 */
public final class SceneStatus extends GenericStatusMessage implements SceneStatuses {
    private static final int SCENE_STATUS_MANDATORY_LENGTH = 3;
    private static final String TAG = SceneStatus.class.getSimpleName();
    private static final int OP_CODE = SCENE_STATUS;
    private int mStatusCode;
    private int mCurrentScene;
    private int mTargetScene;
    private int mRemainingTime;
    private int mTransitionSteps;
    private int mTransitionResolution;

    public static final Logger LOG = Logger.getLogger(SceneStatus.class.getName());

    /**
     * Constructs the GenericOnOffStatus mMessage.
     *
     * @param message Access Message
     */
    public SceneStatus(final AccessMessage message) {
        super(message);
        this.mMessage = message;
        this.mParameters = message.getParameters();
        parseStatusParameters();
    }

    @Override
    void parseStatusParameters() {
        LOG.info("Received scene status from: " + MeshAddress.formatAddress(mMessage.getSrc(), true));
        final ByteBuffer buffer = ByteBuffer.wrap(mParameters).order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(0);
        mStatusCode = buffer.get() & 0xFF;
        mCurrentScene = buffer.getShort() & 0xFFFF;
        LOG.info("Status: " + mStatusCode);
        LOG.info("Current Scene : " + mCurrentScene);
        if (buffer.limit() > SCENE_STATUS_MANDATORY_LENGTH) {
            mTargetScene = buffer.getShort() & 0xFFFF;
            mRemainingTime = buffer.get() & 0xFF;
            mTransitionSteps = (mRemainingTime & 0x3F);
            mTransitionResolution = (mRemainingTime >> 6);
            LOG.info("Target scene: " + mTargetScene);
            LOG.info("Remaining time, transition number of steps: " + mTransitionSteps);
            LOG.info("Remaining time, transition number of step resolution: " + mTransitionResolution);
            LOG.info("Remaining time: " + MeshParserUtils.getRemainingTime(mRemainingTime));
        }
    }

    @Override
    public int getOpCode() {
        return OP_CODE;
    }

    /**
     * Returns the present state of the GenericOnOffModel
     *
     * @return true if on and false other wise
     */
    public final int getStatus() {
        return mStatusCode;
    }

    /**
     * Returns true if the message was successful.
     */
    public final boolean isSuccessful() {
        return mStatusCode == 0;
    }

    /**
     * Returns the target state of the GenericOnOffModel
     *
     * @return true if on and false other wise
     */
    public final int getCurrentScene() {
        return mCurrentScene;
    }

    /**
     * Returns the target state of the GenericOnOffModel
     *
     * @return true if on and false other wise
     */
    public final Integer getTargetScene() {
        return mTargetScene;
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

