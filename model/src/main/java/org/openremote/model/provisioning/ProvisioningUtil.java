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

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProvisioningUtil {

    protected static final Logger LOG = Logger.getLogger(ProvisioningUtil.class.getName());
    protected static CertificateFactory certificateFactory;

    static {
        try {
            certificateFactory = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            LOG.log(Level.SEVERE, "Failed to create X.509 certificate factory", e);
        }
    }

    protected ProvisioningUtil() {
    }

    public static X509Certificate getX509Certificate(String pem) throws CertificateException {
        X509Certificate certificate = null;

        try (InputStream stream = new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8))) {
            certificate = (X509Certificate) certificateFactory.generateCertificate(stream);
        } catch (IOException ignored) {
        }

        return certificate;
    }

    public static String getSubjectCN(X500Principal principal) {
        // Use LDAP RFC 2253 which is same spec as X500 principal to get CN
        try {
            LdapName ldapName = new LdapName(principal.getName());
            return ldapName.getRdns().stream().filter(rdn -> "CN".equals(rdn.getType())).map(rdn -> rdn.getValue().toString()).findFirst().orElse(null);
        } catch (InvalidNameException e) {
            LOG.log(Level.WARNING, "Failed to extract subject CN from X500 principal", e);
            return null;
        }
    }
}
