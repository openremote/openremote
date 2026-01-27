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
package org.openremote.agent.protocol.tradfri.util;

/** The class that contains the credentials used to authenticate to the IKEA TRÅDFRI gateway */
public class Credentials {

  /** The identity that can be used to authenticate to the IKEA TRÅDFRI gateway */
  private String identity;

  /** The key that can be used to authenticate to the IKEA TRÅDFRI gateway */
  private String key;

  /**
   * Construct the Credentials class
   *
   * @param identity The identity that can be used to authenticate to the IKEA TRÅDFRI gateway
   * @param key The key that can be used to authenticate to the IKEA TRÅDFRI gateway
   */
  public Credentials(String identity, String key) {
    this.identity = identity;
    this.key = key;
  }

  /** Construct the Credentials class */
  public Credentials() {}

  /**
   * Get the identity that can be used to authenticate to the IKEA TRÅDFRI gateway
   *
   * @return The identity that can be used to authenticate to the IKEA TRÅDFRI gateway
   */
  public String getIdentity() {
    return this.identity;
  }

  /**
   * Get the key that can be used to authenticate to the IKEA TRÅDFRI gateway
   *
   * @return The key that can be used to authenticate to the IKEA TRÅDFRI gateway
   */
  public String getKey() {
    return this.key;
  }

  /**
   * Set the identity that can be used to authenticate to the IKEA TRÅDFRI gateway
   *
   * @param identity The new identity that can be used to authenticate to the IKEA TRÅDFRI gateway
   */
  public void setIdentity(String identity) {
    this.identity = identity;
  }

  /**
   * Set the key that can be used to authenticate to the IKEA TRÅDFRI gateway
   *
   * @param key The new key that can be used to authenticate to the IKEA TRÅDFRI gateway
   */
  public void setKey(String key) {
    this.key = key;
  }
}
