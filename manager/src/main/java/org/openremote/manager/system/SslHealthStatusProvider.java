/*
 * Copyright 2018, OpenRemote Inc.
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
package org.openremote.manager.system;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.system.HealthStatusProvider;
import org.openremote.model.value.Values;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.openremote.container.security.keycloak.KeycloakIdentityProvider.*;
import static org.openremote.container.util.MapAccess.getBoolean;
import static org.openremote.container.util.MapAccess.getInteger;

public class SslHealthStatusProvider implements X509TrustManager, HealthStatusProvider, ContainerService {

    public static final String NAME = "ssl";
    public static final String VERSION = "1.0";
    protected static final Logger LOG = Logger.getLogger(SslHealthStatusProvider.class.getName());
    protected boolean sslEnabled;
    protected String hostname;
    protected int port;
    protected SSLContext sslContext;

    @Override
    public int getPriority() {
        return ContainerService.DEFAULT_PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {
        sslEnabled = getBoolean(container.getConfig(), IDENTITY_NETWORK_SECURE, IDENTITY_NETWORK_SECURE_DEFAULT);
        hostname = container.getConfig().getOrDefault(IDENTITY_NETWORK_HOST, IDENTITY_NETWORK_HOST_DEFAULT);
        port = getInteger(container.getConfig(), IDENTITY_NETWORK_WEBSERVER_PORT, IDENTITY_NETWORK_WEBSERVER_PORT_DEFAULT);
    }

    @Override
    public void start(Container container) throws Exception {
        if (sslEnabled) {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{this}, null);
        }
    }

    @Override
    public void stop(Container container) throws Exception {

    }

    @Override
    public String getHealthStatusName() {
        return NAME;
    }

    @Override
    public String getHealthStatusVersion() {
        return VERSION;
    }

    @Override
    public Object getHealthStatus() {
        if (!sslEnabled) {
            return null;
        }

        SSLSocketFactory ssf = sslContext.getSocketFactory();

        try {
            SSLSocket socket = (SSLSocket) ssf.createSocket(hostname, 443);
            socket.startHandshake();

            X509Certificate[] peerCertificates = (X509Certificate[]) socket.getSession().getPeerCertificates();
            X509Certificate serverCert = peerCertificates[0];

            Date date = serverCert.getNotAfter();
            long validDays = DAYS.between(Instant.now(), date.toInstant());
            ObjectNode objectValue = Values.JSON.createObjectNode();
            objectValue.put("validDays", validDays);
            return objectValue;
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to connect to SSL port 443 on host: " + hostname);
            return null;
        }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return null;
    }
}
