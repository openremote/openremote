/*
 * Copyright 2021, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.agent.protocol.bluetooth.mesh.transport;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Logger;

import org.openremote.agent.protocol.bluetooth.mesh.opcodes.ApplicationMessageOpCodes;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshAddress;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;

/** To be used as a wrapper class for when creating the GenericOnOffStatus Message. */
public final class GenericOnOffStatus extends GenericStatusMessage {

  public static final Logger LOG = Logger.getLogger(GenericOnOffStatus.class.getName());
  private static final int OP_CODE = ApplicationMessageOpCodes.GENERIC_ON_OFF_STATUS;
  private static final int GENERIC_ON_OFF_STATE_ON = 0x01;
  private boolean mPresentOn;
  private Boolean mTargetOn;
  private int mRemainingTime;
  private int mTransitionSteps;
  private int mTransitionResolution;

  /**
   * Constructs the GenericOnOffStatus mMessage.
   *
   * @param message Access Message
   */
  public GenericOnOffStatus(final AccessMessage message) {
    super(message);
    this.mParameters = message.getParameters();
    parseStatusParameters();
  }

  @Override
  void parseStatusParameters() {
    LOG.info(
        "Received generic on off status from: "
            + MeshAddress.formatAddress(mMessage.getSrc(), true));
    final ByteBuffer buffer = ByteBuffer.wrap(mParameters).order(ByteOrder.LITTLE_ENDIAN);
    buffer.position(0);
    mPresentOn = buffer.get() == GENERIC_ON_OFF_STATE_ON;
    LOG.info("Present on: " + mPresentOn);
    if (buffer.limit() > 1) {
      mTargetOn = buffer.get() == GENERIC_ON_OFF_STATE_ON;
      mRemainingTime = buffer.get() & 0xFF;
      mTransitionSteps = (mRemainingTime & 0x3F);
      mTransitionResolution = (mRemainingTime >> 6);
      LOG.info("Target on: " + mTargetOn);
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
  public final boolean getPresentState() {
    return mPresentOn;
  }

  /**
   * Returns the target state of the GenericOnOffModel
   *
   * @return true if on and false other wise
   */
  public final Boolean getTargetState() {
    return mTargetOn;
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
