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
 * To be used as a wrapper class when creating a GenericOnOffSet message.
 */
@SuppressWarnings("unused")
public class GenericOnOffSet extends GenericMessage {

    public static final Logger LOG = Logger.getLogger(GenericOnOffSet.class.getName());
    private static final int OP_CODE = ApplicationMessageOpCodes.GENERIC_ON_OFF_SET;
    private static final int GENERIC_ON_OFF_SET_TRANSITION_PARAMS_LENGTH = 4;
    private static final int GENERIC_ON_OFF_SET_PARAMS_LENGTH = 2;

    private final Integer mTransitionSteps;
    private final Integer mTransitionResolution;
    private final Integer mDelay;
    private final boolean mState;
    private final int tId;

    /**
     * Constructs GenericOnOffSet message.
     *
     * @param appKey {@link ApplicationKey} key for this message
     * @param state  Boolean state of the GenericOnOffModel
     * @param tId    Transaction id
     * @throws IllegalArgumentException if any illegal arguments are passed
     */
    public GenericOnOffSet(final ApplicationKey appKey,
                           final boolean state,
                           final int tId) throws IllegalArgumentException {
        this(appKey, state, tId, null, null, null);
    }

    /**
     * Constructs GenericOnOffSet message.
     *
     * @param appKey               {@link ApplicationKey} key for this message
     * @param state                Boolean state of the GenericOnOffModel
     * @param tId                  Transaction id
     * @param transitionSteps      Transition steps for the level
     * @param transitionResolution Transition resolution for the level
     * @param delay                Delay for this message to be executed 0 - 1275 milliseconds
     * @throws IllegalArgumentException if any illegal arguments are passed
     */
    public GenericOnOffSet(final ApplicationKey appKey,
                           final boolean state,
                           final int tId,
                           /* @Nullable */ final Integer transitionSteps,
                           /* @Nullable */ final Integer transitionResolution,
                           /* @Nullable */ final Integer delay) {
        super(appKey);
        this.mTransitionSteps = transitionSteps;
        this.mTransitionResolution = transitionResolution;
        this.mDelay = delay;
        this.mState = state;
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
        LOG.info("State: " + (mState ? "ON" : "OFF"));
        if (mTransitionSteps == null || mTransitionResolution == null || mDelay == null) {
            paramsBuffer = ByteBuffer.allocate(GENERIC_ON_OFF_SET_PARAMS_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
            paramsBuffer.put((byte) (mState ? 0x01 : 0x00));
            paramsBuffer.put((byte) tId);
        } else {
            LOG.info("Transition steps: " + mTransitionSteps);
            LOG.info("Transition step resolution: " + mTransitionResolution);
            paramsBuffer = ByteBuffer.allocate(GENERIC_ON_OFF_SET_TRANSITION_PARAMS_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
            paramsBuffer.put((byte) (mState ? 0x01 : 0x00));
            paramsBuffer.put((byte) tId);
            paramsBuffer.put((byte) (mTransitionResolution << 6 | mTransitionSteps));
            final int delay = mDelay;
            paramsBuffer.put((byte) delay);
        }
        mParameters = paramsBuffer.array();

    }
}

