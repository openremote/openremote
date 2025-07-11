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

import java.util.ArrayList;
import java.util.List;

import org.openremote.agent.protocol.bluetooth.mesh.opcodes.ProxyConfigMessageOpCodes;
import org.openremote.agent.protocol.bluetooth.mesh.utils.AddressArray;

/** To be used as a wrapper class to create the ProxyConfigSetFilterType message. */
public class ProxyConfigAddAddressToFilter extends ProxyConfigMessage {

  private final List<AddressArray> addresses;

  /**
   * Sets the proxy filter
   *
   * @param addresses List of addresses to be added to the filter
   */
  public ProxyConfigAddAddressToFilter(final List<AddressArray> addresses)
      throws IllegalArgumentException {
    this.addresses = new ArrayList<>();
    this.addresses.addAll(addresses);
    assembleMessageParameters();
  }

  @Override
  void assembleMessageParameters() throws IllegalArgumentException {
    if (addresses.isEmpty()) throw new IllegalArgumentException("Address list cannot be empty!");
    final int length = (int) Math.pow(2, addresses.size());
    mParameters = new byte[length];
    int count = 0;
    for (AddressArray addressArray : addresses) {
      mParameters[count] = addressArray.getAddress()[0];
      mParameters[count + 1] = addressArray.getAddress()[1];
      count += 2;
    }
  }

  @Override
  public int getOpCode() {
    return ProxyConfigMessageOpCodes.ADD_ADDRESS;
  }

  @Override
  byte[] getParameters() {
    return mParameters;
  }

  /** Returns the addresses that were added to the proxy filter */
  public List<AddressArray> getAddresses() {
    return addresses;
  }
}
