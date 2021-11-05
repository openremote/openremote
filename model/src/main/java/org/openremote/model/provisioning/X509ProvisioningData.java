/*
 * Copyright 2021, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.model.provisioning;

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
    public String toString() {
        return getClass().getSimpleName() +"{" +
            "CACertPEM='" + (CACertPEM != null ? CACertPEM.substring(0, 20) + "..." : null) + '\'' +
            ", ignoreExpiryDate=" + ignoreExpiryDate +
            '}';
    }
}
