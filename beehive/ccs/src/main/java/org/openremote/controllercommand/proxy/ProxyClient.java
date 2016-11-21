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
package org.openremote.controllercommand.proxy;

import org.openremote.controllercommand.ControllerProxyAndCommandServiceApplication;
import org.openremote.controllercommand.domain.InitiateProxyControllerCommand;
import org.openremote.controllercommand.domain.User;
import org.openremote.controllercommand.service.AccountService;
import org.openremote.controllercommand.service.ControllerCommandService;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLServerSocketFactory;
import javax.persistence.EntityManager;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProxyClient extends Proxy {

    protected final static org.slf4j.Logger logger = LoggerFactory.getLogger(ProxyServer.class);

    private ProxyServer server;
    private String hostName;
    private int minClientPort;
    private int maxClientPort;
    private AccountService accountService;
    private ControllerCommandService controllerCommandService;
    private SSLServerSocketFactory sslServerSocketFactory;
    private ControllerProxyAndCommandServiceApplication application;

    public ProxyClient(ProxyServer server, Socket clientSocket, int timeout, String hostName, int minClientPort, int maxClientPort,
                       ControllerCommandService controllerCommandService, AccountService accountService, SSLServerSocketFactory sslServerSocketFactory, ControllerProxyAndCommandServiceApplication application) throws IOException {
        super(clientSocket, timeout);
        this.server = server;
        this.hostName = hostName;
        this.minClientPort = minClientPort;
        this.maxClientPort = maxClientPort;
        this.accountService = accountService;
        this.controllerCommandService = controllerCommandService;
        this.sslServerSocketFactory = sslServerSocketFactory;
        this.application = application;
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
            logger.info("Binding socket for client");
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
            logger.info("Socket bound to port " + localPort);

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
                    logger.info("We have a client, now let's check the token");
                    // this throws on error
                    checkToken(clientSocket, controllerCommand);
                    return clientSocket;
                }
                // we timed out
            } finally {
                // we got contacted, or not but let's drop this command since we're not listening anymore
                EntityManager entityManager = application.createEntityManager();
                try {
                    ControllerCommandService controllerCommandService = getControllerCommandService();
                    controllerCommandService.closeControllerCommand(controllerCommand);
                    controllerCommandService.update(entityManager, controllerCommand);
                    application.commitEntityManager(entityManager);
                    entityManager = null;
                } finally {
                    if (entityManager != null) {
                        application.rollbackEntityManager(entityManager);
                    }
                }
            }
        } finally {
            try {
                serverSocket.close();
            } catch (IOException x) {
                // ignore in finally
            }

        }
        logger.info("Halted");
        throw new IOException("We've been halted");
    }

    private void checkToken(Socket clientSocket, InitiateProxyControllerCommand controllerCommand) throws IOException {
        try {
            byte[] token = controllerCommand.getToken().getBytes("ASCII");
            byte[] readToken = new byte[token.length];
            clientSocket.getInputStream().read(readToken);
            logger.info("Checking token");
            // we have read it all, now compare
            for (int i = 0; i < token.length; i++) {
                if (token[i] != readToken[i]) {
                    throw new IOException("Client connected with invalid token");
                }
            }
            logger.info("Token is good");
            return;
        } catch (IOException x) {
            clientSocket.close();
            throw x;
        }
    }

    private User authenticateUser() throws IOException {
        // we need to read data from the client until we get the Authentication header
        try {
            logger.info("Reading HTTP headers");
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
        String value = matcher.group(1);
        // now fold it
        value = value.replaceAll("\r\n[ \t]+", " ").trim();
        // and attempt to validate it
        User user = null;
        EntityManager entityManager = application.createEntityManager();
        try {
            user = getAccountService().loadByHTTPBasicCredentials(entityManager, value);
        } finally {
            application.rollbackEntityManager(entityManager);
        }
        if (user == null) {
            // authentication failed
            throw new HTTPException(HttpURLConnection.HTTP_UNAUTHORIZED, json, optionsRequest);
        }
        return user;
    }

    protected ControllerCommandService getControllerCommandService() {
        return this.controllerCommandService;
    }

    protected AccountService getAccountService() {
        return this.accountService;
    }

    private InitiateProxyControllerCommand contactController(User user, int port) {
        InitiateProxyControllerCommand command = null;
        EntityManager entityManager = application.createEntityManager();
        try {
            ControllerCommandService controllerCommandService = getControllerCommandService();
            command = controllerCommandService.saveProxyControllerCommand(entityManager, user, ((sslServerSocketFactory != null) ? "https://" : "http://") + hostName + ":" + port);
            application.commitEntityManager(entityManager);
            entityManager = null;
        } finally {
            if (entityManager != null) {
                application.rollbackEntityManager(entityManager);
            }
        }
        return command;
    }

}
