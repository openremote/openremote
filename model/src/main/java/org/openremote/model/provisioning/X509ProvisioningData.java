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
package org.openremote.model.provisioning;

import java.util.Objects;

public class X509ProvisioningData {

  protected String CACertPEM;
  protected boolean ignoreExpiryDate;

  public String getCACertPEM() {
    return CACertPEM;
  }

  public X509ProvisioningData setCACertPEM(String CACertPEM) {
    this.CACertPEM = CACertPEM;
    return this;
  }

  public boolean isIgnoreExpiryDate() {
    return ignoreExpiryDate;
  }

  public X509ProvisioningData setIgnoreExpiryDate(boolean ignoreExpiryDate) {
    this.ignoreExpiryDate = ignoreExpiryDate;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    X509ProvisioningData that = (X509ProvisioningData) o;
    return ignoreExpiryDate == that.ignoreExpiryDate && Objects.equals(CACertPEM, that.CACertPEM);
  }

  @Override
  public int hashCode() {
    return Objects.hash(CACertPEM, ignoreExpiryDate);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "{"
        + "CACertPEM='"
        + (CACertPEM != null ? CACertPEM.substring(0, 20) + "..." : null)
        + '\''
        + ", ignoreExpiryDate="
        + ignoreExpiryDate
        + '}';
  }
}
