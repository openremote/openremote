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
import org.openremote.ccs.model.ControllerCommand;
import org.openremote.ccs.model.InitiateProxyControllerCommand;
import org.openremote.ccs.model.User;

import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProxyClient extends Thread {

    private static final Logger LOG = Logger.getLogger(ProxyClient.class.getName());

    protected Socket consoleSocket;
    protected boolean halted;
    protected int timeout;
    protected byte[] consoleBuffer;
    protected int consoleBytesRead;

    private ProxyServer server;
    private String hostName;
    private int minClientPort;
    private int maxClientPort;
    private CCSPersistenceService persistenceService;
    private SSLServerSocketFactory sslServerSocketFactory;

    public ProxyClient(ProxyServer server, Socket clientSocket, int timeout, String hostName, int minClientPort, int maxClientPort,
                       CCSPersistenceService persistenceService, SSLServerSocketFactory sslServerSocketFactory) throws IOException {
        this.consoleSocket = clientSocket;
        this.timeout = timeout;
        this.server = server;
        this.hostName = hostName;
        this.minClientPort = minClientPort;
        this.maxClientPort = maxClientPort;
        this.persistenceService = persistenceService;
        this.sslServerSocketFactory = sslServerSocketFactory;
    }

    @Override
    public void run() {
        try {
            LOG.info("Client running");
            // we first need to connect to the endpoint
            Socket controllerSocket;
            try {
                controllerSocket = openDestinationSocket();
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Failed to connect to the destination", e);
                return;
            }
            try {
                LOG.info("We got connection to the destination");
                final byte[] request = new byte[1024];
                byte[] reply = new byte[4096];

                // Get client streams. Make them final so they can
                // be used in the anonymous thread below.
                final InputStream from_client = consoleSocket.getInputStream();
                final OutputStream to_client = consoleSocket.getOutputStream();

                // Get server streams.
                final InputStream from_server = controllerSocket.getInputStream();
                final OutputStream to_server = controllerSocket.getOutputStream();

                //Write the HTTP request which was already read when checking the authentication header
                to_server.write(consoleBuffer, 0, consoleBytesRead);
                to_server.flush();

                // Make a thread to read the client's requests and pass them
                // to the server. We have to use a separate thread because
                // requests and responses may be asynchronous.
                Thread t = new Thread() {
                    public void run() {
                        int bytes_read;
                        try {
                            while ((bytes_read = from_client.read(request)) != -1) {
                                to_server.write(request, 0, bytes_read);
                                to_server.flush();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        // the client closed the connection to us, so close our
                        // connection to the server. This will also cause the
                        // server-to-client loop in the main thread exit.
                        try {
                            to_server.close();
                        } catch (IOException e) {

                        }
                    }
                };

                // Start the client-to-server request thread running
                t.start();

                // Meanwhile, in the main thread, read the server's responses
                // and pass them back to the client. This will be done in
                // parallel with the client-to-server request thread above.
                int bytes_read;
                try {
                    while ((bytes_read = from_server.read(reply)) != -1) {
                        to_client.write(reply, 0, bytes_read);
                        to_client.flush();
                    }
                } catch (IOException e) {
                }

                // The server closed its connection to us, so we close our
                // connection to our client.
                // This will make the other thread exit.
                to_client.close();

                LOG.info("Done with proxying");
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Proxy dead", e);
            } finally {
                // close the dst socket
                try {
                    controllerSocket.close();
                } catch (IOException x) {
                }
            }
        } finally {
            // close the src socket
            try {
                consoleSocket.close();
            } catch (IOException x) {
            }
            onProxyExit();
        }
    }

    public void halt() {
        halted = true;
    }

    protected void onProxyExit() {
        server.unregister(this);
    }

    protected Socket openDestinationSocket() throws IOException {
        // this either returns a good user, or throws
        User user = authenticateUser();
        ServerSocket serverSocket;
        if (sslServerSocketFactory != null) {
            serverSocket = sslServerSocketFactory.createServerSocket();
        } else {
            serverSocket = new ServerSocket();
        }
        int localPort;
        try {
            LOG.info("Binding socket for client");
            for (localPort = minClientPort; localPort <= maxClientPort; localPort++) {
                try {
                    serverSocket.bind(new InetSocketAddress(localPort));
                    if (serverSocket.getLocalPort() != -1) {
                        break;
                    }
                } catch (IOException ignoreAndContinue) {
                }
            }

            localPort = serverSocket.getLocalPort();
            if (localPort == -1) {
                throw new IOException("Could not bind local socket between ports " + minClientPort + " and " + maxClientPort);
            }
            LOG.info("Socket bound to port " + localPort);

            // now let's tell the client we want connection here
            InitiateProxyControllerCommand controllerCommand = contactController(user, localPort);
            try {
                serverSocket.setSoTimeout(timeout);
                // now wait for the client to connect
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    if (halted) {
                        break;
                    }
                    // we have a socket, let's cancel the key and return the client socket
                    LOG.info("We have a client, now let's check the token");
                    // this throws on error
                    checkToken(clientSocket, controllerCommand);
                    return clientSocket;
                }
                // we timed out
            } finally {
                // we got contacted, or not but let's drop this command since we're not listening anymore
                persistenceService.doTransaction(entityManager -> {
                    controllerCommand.setState(ControllerCommand.State.DONE);
                    entityManager.merge(controllerCommand);
                });
            }
        } finally {
            try {
                serverSocket.close();
            } catch (IOException x) {
                // ignore in finally
            }

        }
        LOG.info("Halted");
        throw new IOException("We've been halted");
    }

    private void checkToken(Socket clientSocket, InitiateProxyControllerCommand controllerCommand) throws IOException {
        try {
            byte[] token = controllerCommand.getToken().getBytes("ASCII");
            byte[] readToken = new byte[token.length];
            clientSocket.getInputStream().read(readToken);
            LOG.info("Checking token");
            // we have read it all, now compare
            for (int i = 0; i < token.length; i++) {
                if (token[i] != readToken[i]) {
                    throw new IOException("Client connected with invalid token");
                }
            }
            LOG.info("Token is good");
            return;
        } catch (IOException x) {
            clientSocket.close();
            throw x;
        }
    }

    private User authenticateUser() throws IOException {
        // we need to read data from the client until we get the Authentication header
        try {
            LOG.info("Reading HTTP headers");
            if (halted) {
                throw new IOException("Halted");
            }
            consoleBuffer = new byte[4096];
            consoleBytesRead = consoleSocket.getInputStream().read(consoleBuffer);
            if (consoleBytesRead == -1) {
                // we've reached EOF, drop this client
                throw new HTTPException(HttpURLConnection.HTTP_UNAUTHORIZED, false, false);
            }
           
           /* We read some more if we only receive 1 byte
            * see here: http://stackoverflow.com/questions/9358424/java-ssl-chrome-firefox-sends-g-in-http-header-instead-of-get-http-1-1
            */
            if (consoleBytesRead == 1) {
                consoleBytesRead = consoleSocket.getInputStream().read(consoleBuffer, 1, 4096);
                consoleBytesRead += 1;
            }

            // we have new data, let's look at it
            User user = getAuthenticatedUser(consoleBuffer, consoleBytesRead);
            if (user != null) {
                return user;
            }
            // for a timeout or were halted we just close the connection upstream by throwing
            throw new IOException("Connection timed-out before we could read the authentication header");
        } catch (HTTPException x) {
            // we must reply with an error
            throw sendError(consoleSocket, x.getStatus(), x.isJson(), x.isOptionsRequest());
        }
    }

    private IOException sendError(Socket srcSocket, int status, boolean json, boolean optionsRequest) throws IOException {
        // Construct the message
        String reason;
        String maybeHeader = "";
        String body = "";

        maybeHeader = "Access-Control-Allow-Origin: *\r\n";
        maybeHeader = maybeHeader + "Access-Control-Allow-Methods: GET, POST\r\n";
        maybeHeader = maybeHeader + "Access-Control-Allow-Headers: origin, authorization, accept\r\n";
        maybeHeader = maybeHeader + "Access-Control-Max-Age: 99999\r\n";

        switch (status) {
            case HttpURLConnection.HTTP_BAD_REQUEST:
                reason = "Bad request";
                break;
            case HttpURLConnection.HTTP_UNAUTHORIZED:
                reason = "Unauthorized";
                if (json) {
                    status = 200;
                    reason = "OK";
                    body = "{\"error\": {\"code\": 401,\"message\": \"Not Authorized\"}}";
                } else {
                    if (!optionsRequest) {
                        maybeHeader = maybeHeader + "WWW-Authenticate: Basic realm=\"OPENREMOTE_Beehive\"\r\n";
                    }
                }
                break;
            case HttpURLConnection.HTTP_FORBIDDEN:
                reason = "Forbidden";
                break;
            default:
                reason = "Unknown error";
        }
        if (optionsRequest) {
            status = 200;
            reason = "OK";
        }
        String response = "HTTP/1.1 " + status + " " + reason + "\r\n" + maybeHeader;
        if (body.length() != 0) {
            response = response + "Content-Length: " + body.length() + "\r\n";
        } else {
            response = response + "\r\n";
        }
        response = response + "\r\n" + body;

        byte[] dataToSend = response.getBytes("ASCII");
        // now send it
        OutputStream out = srcSocket.getOutputStream();
        out.write(dataToSend);
        out.flush();
        // we're done, let bail
        return new IOException("Error message (most likely wrong authentication data) was sent to client.");
    }

    private User getAuthenticatedUser(byte[] bytes, int length) throws HTTPException {
        // http://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4 says we should look for \r\nAuthorization:(.*)\r\n([^ \t]) as long
        // as it's not preceded by "\r\n\r\n" which indicates the end of the header section
        // we cannot parse data after the headers, but the headers are always in ASCII, so find that limit first
        int limit = length;
        int headerEnd = limit;
        for (int i = 0; i < limit - 3; i++) {
            if (bytes[i] == '\r'
                && bytes[i + 1] == '\n'
                && bytes[i + 2] == '\r'
                && bytes[i + 3] == '\n') {
                headerEnd = i + 4;
                break;
            }
        }
        // in any case, non-ASCII is after the end of headers, or since we haven't reached that yet, after the whole limit
        // so we can parse as ASCII characters
        String headers = new String(bytes, 0, headerEnd, Charset.forName("ASCII"));
        boolean optionsRequest = false;
        if (headers.startsWith("OPTIONS")) {
            optionsRequest = true;
        }
        // so do we have an Authentication header in there?
        Pattern pattern = Pattern.compile("\r\nAuthorization: ([^\r\n]*)\r\n([^ \t])", Pattern.CASE_INSENSITIVE);
        Pattern pattern2 = Pattern.compile("\r\nAccept:([^\r\n]*)\r\n([^ \t])", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(headers);
        Matcher matcher2 = pattern2.matcher(headers);
        boolean json = false;
        if (matcher2.find()) {
            if (matcher2.group(1).toLowerCase().indexOf("json") != -1) {
                json = true;
            }
        }
        // nothing?
        if (!matcher.find()) {
            if (headerEnd <= limit) {
                // we have seen every header, and found no good one, let's quit
                throw new HTTPException(HttpURLConnection.HTTP_UNAUTHORIZED, json, optionsRequest);
            }
            // we haven't seen the end of headers yet, don't give up and read more
            return null;
        }
        // we have a value!
        String credentials = matcher.group(1).replaceAll("\r\n[ \t]+", " ").trim();
        // and attempt to validate it
        User user = persistenceService.doReturningTransaction(
            entityManager -> persistenceService.loadByHTTPBasicCredentials(entityManager, credentials)
        );
        if (user == null) {
            // authentication failed
            throw new HTTPException(HttpURLConnection.HTTP_UNAUTHORIZED, json, optionsRequest);
        }
        return user;
    }

    private InitiateProxyControllerCommand contactController(User user, int port) {
        return persistenceService.doReturningTransaction(
            entityManager -> persistenceService.saveProxyControllerCommand(
                entityManager,
                user,
                ((sslServerSocketFactory != null) ? "https://" : "http://") + hostName + ":" + port
            )
        );
    }
}
