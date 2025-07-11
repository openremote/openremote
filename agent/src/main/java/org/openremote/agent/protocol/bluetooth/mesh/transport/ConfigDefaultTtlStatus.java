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

import org.openremote.agent.protocol.bluetooth.mesh.opcodes.ConfigMessageOpCodes;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;

/** Creates the ConfigDefaultTtlSet message. */
public class ConfigDefaultTtlStatus extends ConfigStatusMessage {

  private static final String TAG = ConfigDefaultTtlStatus.class.getSimpleName();
  private static final int OP_CODE = ConfigMessageOpCodes.CONFIG_DEFAULT_TTL_STATUS;

  private int mTtl;

  /**
   * Constructs ConfigDefaultTtlStatus message.
   *
   * @param message {@link AccessMessage}
   */
  public ConfigDefaultTtlStatus(final AccessMessage message) {
    super(message);
    mParameters = message.getParameters();
    parseStatusParameters();
  }

  @Override
  void parseStatusParameters() {
    mTtl = MeshParserUtils.unsignedByteToInt(mParameters[0]);
  }

  @Override
  public int getOpCode() {
    return OP_CODE;
  }

  /** Returns the ttl value */
  public int getTtl() {
    return mTtl;
  }
}
