/*
 * Copyright 2025, OpenRemote Inc.
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
package org.openremote.model.services;

/**
 * Lease info for an external service instance, contains timestamps for lease expiration,
 * registration and renewal.
 */
public class ExternalServiceLeaseInfo {

  /** The user's userId that registered the service */
  private String registrarUserId;

  /** The timestamp when the lease expires. */
  private long expirationTimestamp;

  /** The timestamp when the lease was registered. */
  private long registrationTimestamp;

  /** The timestamp when the lease was renewed. */
  private long renewalTimestamp;

  public ExternalServiceLeaseInfo(
      String registrarUserId,
      long expirationTimestamp,
      long registrationTimestamp,
      long renewalTimestamp) {
    this.registrarUserId = registrarUserId;
    this.expirationTimestamp = expirationTimestamp;
    this.registrationTimestamp = registrationTimestamp;
    this.renewalTimestamp = renewalTimestamp;
  }

  public void setExpirationTimestamp(long expirationTimestamp) {
    this.expirationTimestamp = expirationTimestamp;
  }

  public long getExpirationTimestamp() {
    return expirationTimestamp;
  }

  public long getRegistrationTimestamp() {
    return registrationTimestamp;
  }

  public void setRegistrationTimestamp(long registrationTimestamp) {
    this.registrationTimestamp = registrationTimestamp;
  }

  public long getRenewalTimestamp() {
    return renewalTimestamp;
  }

  public void setRenewalTimestamp(long renewalTimestamp) {
    this.renewalTimestamp = renewalTimestamp;
  }

  public boolean isExpired(long currentTime) {
    return expirationTimestamp < currentTime;
  }

  public String getRegistrarUserId() {
    return registrarUserId;
  }

  public void setRegistrarUserId(String registrarUserId) {
    this.registrarUserId = registrarUserId;
  }

  @Override
  public String toString() {
    return "ExternalServiceLeaseInfo{"
        + ", registrarUserId="
        + registrarUserId
        + ", expirationTimestamp="
        + expirationTimestamp
        + ", registrationTimestamp="
        + registrationTimestamp
        + ", renewalTimestamp="
        + renewalTimestamp
        + '}';
  }
}
