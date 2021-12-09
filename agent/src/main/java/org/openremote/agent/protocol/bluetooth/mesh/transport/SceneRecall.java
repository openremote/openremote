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

import org.openremote.agent.protocol.bluetooth.mesh.ApplicationKey;
import org.openremote.agent.protocol.bluetooth.mesh.opcodes.ApplicationMessageOpCodes;
import org.openremote.agent.protocol.bluetooth.mesh.utils.SecureUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Logger;

/**
 * To be used as a wrapper class when creating a SceneStore message.
 */
public class SceneRecall extends GenericMessage {

    public static final Logger LOG = Logger.getLogger(SceneRecall.class.getName());
    private static final int OP_CODE = ApplicationMessageOpCodes.SCENE_RECALL;
    private static final int SCENE_RECALL_TRANSITION_PARAMS_LENGTH = 5;
    private static final int SCENE_RECALL_PARAMS_LENGTH = 3;

    private final Integer mTransitionSteps;
    private final Integer mTransitionResolution;
    private final Integer mDelay;
    private final int mSceneNumber;
    private final int tId;

    /**
     * Constructs SceneStore message.
     *
     * @param appKey      {@link ApplicationKey} key for this message
     * @param sceneNumber SceneNumber
     * @param tId         Transaction Id
     * @throws IllegalArgumentException if any illegal arguments are passed
     */
    public SceneRecall(final ApplicationKey appKey,
                       final int sceneNumber,
                       final int tId) throws IllegalArgumentException {
        this(appKey, null, null, null, sceneNumber, tId);
    }

    /**
     * Constructs SceneStore message.
     *
     * @param appKey               {@link ApplicationKey} key for this message
     * @param transitionSteps      Transition steps for the level
     * @param transitionResolution Transition resolution for the level
     * @param delay                Delay for this message to be executed 0 - 1275 milliseconds
     * @param sceneNumber          sceneNumber
     * @param tId                  Transaction Id
     * @throws IllegalArgumentException if any illegal arguments are passed
     */
    @SuppressWarnings("WeakerAccess")
    public SceneRecall(final ApplicationKey appKey,
                       /* @Nullable */ final Integer transitionSteps,
                       /* @Nullable */ final Integer transitionResolution,
                       /* @Nullable */ final Integer delay,
                       final int sceneNumber,
                       final int tId) {
        super(appKey);
        this.mTransitionSteps = transitionSteps;
        this.mTransitionResolution = transitionResolution;
        this.mDelay = delay;
        this.mSceneNumber = sceneNumber;
        this.tId = tId;
        assembleMessageParameters();
    }

    @Override
    public int getOpCode() {
        return OP_CODE;
    }

    @Override
    void assembleMessageParameters() {
        mAid = SecureUtils.calculateK4(mAppKey.getKey());
        final ByteBuffer paramsBuffer;
        LOG.info("Scene number: " + mSceneNumber);
        if (mTransitionSteps == null || mTransitionResolution == null || mDelay == null) {
            paramsBuffer = ByteBuffer.allocate(SCENE_RECALL_PARAMS_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
            paramsBuffer.putShort((short) mSceneNumber);
            paramsBuffer.put((byte) tId);
        } else {
            LOG.info("Transition steps: " + mTransitionSteps);
            LOG.info("Transition step resolution: " + mTransitionResolution);
            paramsBuffer = ByteBuffer.allocate(SCENE_RECALL_TRANSITION_PARAMS_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
            paramsBuffer.putShort((short) mSceneNumber);
            paramsBuffer.put((byte) tId);
            paramsBuffer.put((byte) (mTransitionResolution << 6 | mTransitionSteps));
            final int delay = mDelay;
            paramsBuffer.put((byte) delay);
        }
        mParameters = paramsBuffer.array();
    }
}

