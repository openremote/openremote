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

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.openremote.model.util.TextUtil;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import static org.openremote.model.Constants.PERSISTENCE_JSON_VALUE_TYPE;

@Entity
@DiscriminatorValue("x509")
public class X509ProvisioningConfig extends ProvisioningConfig<X509ProvisioningData, X509ProvisioningConfig> {

    @JsonIgnore
    @Transient
    protected X509Certificate certificate;

    @JsonIgnore
    @Transient
    boolean valid = true;

    @Column(name = DATA_PROPERTY_NAME, columnDefinition = "jsonb")
    @org.hibernate.annotations.Type(type = PERSISTENCE_JSON_VALUE_TYPE)
    protected X509ProvisioningData data;

    protected X509ProvisioningConfig() {}

    @Override
    public X509ProvisioningData getData() {
        return data;
    }

    @Override
    public X509ProvisioningConfig setData(X509ProvisioningData data) {
        this.data = data;
        return this;
    }

    public X509ProvisioningConfig(String name, X509ProvisioningData data) {
        super(name);
        this.data = data;
    }

    public synchronized X509Certificate getCertificate() throws IllegalStateException, CertificateException {
        if (!valid) {
            return null;
        }
        if (data == null || TextUtil.isNullOrEmpty(data.getCACertPEM())) {
            valid = false;
            throw new IllegalStateException("CA cert PEM must be defined");
        }
        if (certificate == null) {
            try {
                certificate = ProvisioningUtil.getX509Certificate(data.getCACertPEM());
            } catch (CertificateException e) {
                valid = false;
                throw(e);
            }
        }
        return certificate;
    }
}
