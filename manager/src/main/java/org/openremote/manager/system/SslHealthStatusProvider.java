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
import org.openremote.model.Constants;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.system.HealthStatusProvider;
import org.openremote.model.util.ValueUtil;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.openremote.container.util.MapAccess.getInteger;
import static org.openremote.container.util.MapAccess.getString;

public class SslHealthStatusProvider implements X509TrustManager, HealthStatusProvider, ContainerService {

    public static final String NAME = "ssl";
    public static final String VERSION = "1.0";
    protected static final Logger LOG = Logger.getLogger(SslHealthStatusProvider.class.getName());
    protected String host;
    protected int SSLPort;
    protected SSLContext SSLContext;

    @Override
    public int getPriority() {
        return ContainerService.DEFAULT_PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {
        
        int SSLPort = getInteger(container.getConfig(), Constants.OR_SSL_PORT, -1);
        
        if (SSLPort > 0 && SSLPort <= 65536) {
            this.SSLPort = SSLPort;
            host = getString(container.getConfig(), Constants.OR_HOSTNAME, null);
            SSLContext = SSLContext.getInstance("TLS");
            SSLContext.init(null, new TrustManager[]{this}, null);
        }
    }

    @Override
    public void start(Container container) throws Exception {
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
        if (SSLContext == null) {
            return null;
        }

        SSLSocketFactory ssf = SSLContext.getSocketFactory();

        try {
            SSLSocket socket = (SSLSocket) ssf.createSocket(host, SSLPort);
            socket.startHandshake();

            X509Certificate[] peerCertificates = (X509Certificate[]) socket.getSession().getPeerCertificates();
            X509Certificate serverCert = peerCertificates[0];

            Date date = serverCert.getNotAfter();
            long validDays = DAYS.between(Instant.now(), date.toInstant());
            ObjectNode objectValue = ValueUtil.JSON.createObjectNode();
            objectValue.put("validDays", validDays);
            return objectValue;
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to connect to SSL port " + SSLPort + " on host: " + host);
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
