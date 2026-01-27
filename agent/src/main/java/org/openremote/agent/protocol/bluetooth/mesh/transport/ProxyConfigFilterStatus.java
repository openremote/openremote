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

import org.openremote.agent.protocol.bluetooth.mesh.opcodes.ProxyConfigMessageOpCodes;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;
import org.openremote.agent.protocol.bluetooth.mesh.utils.ProxyFilterType;

/** To be used as a wrapper class to create the ProxyConfigSetFilterType message. */
public class ProxyConfigFilterStatus extends ProxyConfigStatusMessage {
  public static final Logger LOG = Logger.getLogger(ProxyConfigFilterStatus.class.getName());

  private ProxyFilterType mFilterType;
  private int mAddressListSize;

  public ProxyConfigFilterStatus(final ControlMessage controlMessage) {
    super(controlMessage);
    this.mParameters = controlMessage.getParameters();
    parseStatusParameters();
  }

  @Override
  public int getOpCode() {
    return ProxyConfigMessageOpCodes.FILTER_STATUS;
  }

  @Override
  byte[] getParameters() {
    return mParameters;
  }

  @Override
  void parseStatusParameters() {
    mFilterType = new ProxyFilterType(MeshParserUtils.unsignedByteToInt(mParameters[0]));
    // Note proxy protocol is in big endian
    mAddressListSize = MeshParserUtils.unsignedBytesToInt(mParameters[2], mParameters[1]);
    LOG.info("Filter type: " + mFilterType.getFilterTypeName());
    LOG.info("Filter size: " + mAddressListSize);
  }

  /** Returns the {@link ProxyFilterType} set on the proxy */
  public ProxyFilterType getFilterType() {
    return mFilterType;
  }

  /** Returns the size of the address list in the proxy filter */
  public int getListSize() {
    return mAddressListSize;
  }
}
