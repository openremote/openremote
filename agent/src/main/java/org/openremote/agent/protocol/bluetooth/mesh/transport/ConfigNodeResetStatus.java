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

import org.openremote.agent.protocol.bluetooth.mesh.opcodes.ConfigMessageOpCodes;

/** To be used as a wrapper class for when creating the ConfigAppKeyStatus Message. */
@SuppressWarnings({"unused", "WeakerAccess"})
public class ConfigNodeResetStatus extends ConfigStatusMessage {

  public static final Logger LOG = Logger.getLogger(ConfigNodeResetStatus.class.getName());
  private static final int OP_CODE = ConfigMessageOpCodes.CONFIG_NODE_RESET_STATUS;

  /**
   * Constructs the ConfigAppKeyStatus mMessage.
   *
   * @param message Access Message
   */
  public ConfigNodeResetStatus(final AccessMessage message) {
    super(message);
    this.mParameters = message.getParameters();
    parseStatusParameters();
  }

  @Override
  final void parseStatusParameters() {
    // This message has empty parameters
  }

  @Override
  public int getOpCode() {
    return OP_CODE;
  }
}
