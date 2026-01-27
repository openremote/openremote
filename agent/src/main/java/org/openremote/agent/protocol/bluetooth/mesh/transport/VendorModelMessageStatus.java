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

import java.util.logging.Logger;

import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;

/** To be used as a wrapper class for when creating the VendorModelMessageStatus Message. */
public final class VendorModelMessageStatus extends GenericStatusMessage {

  public static final Logger LOG = Logger.getLogger(VendorModelMessageStatus.class.getName());
  private final int mModelIdentifier;

  /**
   * Constructs the VendorModelMessageStatus mMessage.
   *
   * @param message Access Message
   * @param modelIdentifier model identifier
   */
  public VendorModelMessageStatus(final AccessMessage message, final int modelIdentifier) {
    super(message);
    this.mParameters = message.getParameters();
    this.mModelIdentifier = modelIdentifier;
    parseStatusParameters();
  }

  @Override
  void parseStatusParameters() {
    LOG.info("Received Vendor model status: " + MeshParserUtils.bytesToHex(mParameters, false));
  }

  @Override
  public int getOpCode() {
    return mMessage.getOpCode();
  }

  public final byte[] getAccessPayload() {
    return ((AccessMessage) mMessage).getAccessPdu();
  }

  /** Returns the model identifier of model the for this message */
  public int getModelIdentifier() {
    return mModelIdentifier;
  }
}
