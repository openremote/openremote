/*
 * Copyright 2026, OpenRemote Inc.
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
package org.openremote.model.gateway;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openremote.model.event.shared.SharedEvent;

public class GatewayCapabilitiesResponseEvent extends SharedEvent {

  public static final String TYPE = "gateway-capabilities-response";
  protected final boolean tunnelingSupported;

  @JsonCreator
  public GatewayCapabilitiesResponseEvent(
      @JsonProperty("timestamp") Date timestamp,
      @JsonProperty("tunnelingSupported") boolean tunnelingSupported) {
    super(timestamp != null ? timestamp.getTime() : new Date().getTime());
    this.tunnelingSupported = tunnelingSupported;
  }

  public GatewayCapabilitiesResponseEvent(final boolean tunnelingSupported) {
    this.tunnelingSupported = tunnelingSupported;
  }

  public boolean isTunnelingSupported() {
    return tunnelingSupported;
  }
}
