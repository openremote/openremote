/*
 * OpenRemote, the Home of the Digital Home.
 * Copyright 2008-2016, OpenRemote Inc.
 *
 * See the contributors.txt file in the distribution for a
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
package org.openremote.ccs.proxy;

import org.openremote.ccs.CCSPersistenceService;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProxyServer extends Thread {

    private static final Logger LOG = Logger.getLogger(ProxyServer.class.getName());

    private boolean halted = false;
    final private Set<ProxyClient> clients = new HashSet<>();
    private String hostName;
    private int timeout;
    private int port;
    private int minClientPort;
    private int maxClientPort;
    private boolean useSSL;
    private String keystore;
    private String keystorePassword;
    private CCSPersistenceService persistenceService;
    private SSLServerSocketFactory sslServerSocketfactory = null;

    public ProxyServer(String proxyHostname, Integer proxyTimeout, Integer proxyPort, String proxyClientPortRange, Boolean useSSL, String keystore,
                       String keystorePassword, CCSPersistenceService persistenceService) {

        if (proxyHostname == null || proxyHostname.length() == 0) {
            hostName = "localhost";
        } else {
            hostName = proxyHostname;
        }

        if (proxyTimeout == null) {
            timeout = 5000;
        } else {
            timeout = proxyTimeout;
        }

        if (proxyPort == null) {
            port = 10000;
        } else {
            port = proxyPort;
        }

        if (proxyClientPortRange == null || proxyClientPortRange.length() == 0) {
            minClientPort = 0;
            maxClientPort = 0;
        } else {
            minClientPort = Integer.parseInt(proxyClientPortRange.trim().substring(0, proxyClientPortRange.indexOf("-")));
            maxClientPort = Integer.parseInt(proxyClientPortRange.trim().substring(proxyClientPortRange.indexOf("-") + 1, proxyClientPortRange.length()));
        }

        this.useSSL = useSSL != null ? useSSL : false;

        if (this.useSSL && (keystore == null || keystore.length() == 0)) {
            throw new RuntimeException("A keystore has to be specified when using SSL");
        } else {
            this.keystore = keystore;
        }

        if (this.useSSL && (keystorePassword == null || keystorePassword.length() == 0)) {
            throw new RuntimeException("A keystore password has to be specified when using SSL");
        } else {
            this.keystorePassword = keystorePassword;
        }

        if (useSSL) {
            // Specifying the Keystore details
            LOG.info("Load keystore: " + this.keystore);
            InputStream keystoreStream = null;
            try {
                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());

                // Path to keystore starts with a /, consider an absolute path and load as a file instead of a resource
                if (this.keystore.startsWith("/")) {
                    try {
                        keystoreStream = new FileInputStream(this.keystore);
                    } catch (IOException e) {
                        LOG.severe("The specified keystore " + this.keystore + " can not be loaded");
                        throw new RuntimeException("The specified keystore " + this.keystore + " can not be loaded");
                    }
                } else {
                    keystoreStream = this.getClass().getResourceAsStream(this.keystore);
                }
                if (keystoreStream == null) {
                    LOG.severe("The specified keystore " + this.keystore + " can not be loaded");
                    throw new RuntimeException("The specified keystore " + this.keystore + " can not be loaded");
                }

                ks.load(keystoreStream, this.keystorePassword.toCharArray());
                LOG.info("Keystore loaded, it contains " + ks.size() + ((ks.size() > 1) ? " entries" : " entry"));

                trustManagerFactory.init(ks);
                keyManagerFactory.init(ks, this.keystorePassword.toCharArray());

                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
                sslServerSocketfactory = sc.getServerSocketFactory();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if (keystoreStream != null) {
                    try {
                        keystoreStream.close();
                    } catch (IOException e) {
                        LOG.log(Level.WARNING, "Failed to close stream to keystore", e);
                    }
                }
            }
        }

    }

    @Override
    public void run() {
        LOG.info("Proxy server starting up");
        ServerSocket server = null;
        try {
            if (useSSL) {
                // Initialize the SSL Server Socket
                LOG.info("Starting SSL proxy server on port: " + port);
                server = sslServerSocketfactory.createServerSocket(port);
            } else {
                // Create a ServerSocket to listen for incoming console connections
                LOG.info("Starting proxy server on port: " + port);
                server = new ServerSocket(port);
            }

            LOG.info("Entering loop");
            while (true) {
                // Wait for a connection on the local port
                Socket clientSocket = server.accept();
                LOG.info("Accepting a client socket");
                synchronized (clients) {
                    if (halted) {
                        break;
                    }
                    acceptConnection(clientSocket);
                }
            }
            LOG.info("Exited loop");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Server died", e);
        } finally {
            if (server != null)
                try {
                    server.close();
                } catch (IOException e) {
                }
        }
    }

    private void acceptConnection(Socket clientSocket) {
        try {
            LOG.info("Got a client socket");
            ProxyClient client = new ProxyClient(
                this, clientSocket, timeout, hostName,
                minClientPort, maxClientPort,
                persistenceService,
                sslServerSocketfactory
            );
            clients.add(client);
            LOG.info("Starting client");
            client.start();
            LOG.info("Client started");
        } catch (Exception x) {
            // make sure we close this one if needed
            try {
                clientSocket.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    public void halt() {
        synchronized (clients) {
            halted = true;
            for (ProxyClient client : clients) {
                client.halt();
            }
        }
    }

    public void unregister(ProxyClient proxyClient) {
        synchronized (clients) {
            clients.remove(proxyClient);
        }
    }

}
