/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.container.web.socket;

import org.apache.camel.CamelExchangeException;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultProducer;
import org.openremote.container.web.ConnectionConstants;

import javax.websocket.Session;
import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebsocketProducer extends DefaultProducer {

    private static final Logger LOG = Logger.getLogger(WebsocketProducer.class.getName());

    private final Boolean sendToAll;
    private final WebsocketEndpoint endpoint;

    public WebsocketProducer(WebsocketEndpoint endpoint) {
        super(endpoint);
        this.sendToAll = endpoint.getSendToAll();
        this.endpoint = endpoint;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        Message in = exchange.getIn();
        Object message = in.getMandatoryBody();
        if (!(message == null || message instanceof String || message instanceof byte[])) {
            message = in.getMandatoryBody(String.class);
        }
        if (isSendToAllSet(in)) {
            sendToAll(message, exchange);
        } else {
            String sessionKey = in.getHeader(ConnectionConstants.SESSION_KEY, String.class);
            if (sessionKey != null) {
                Session websocket = getEndpoint().getComponent().getWebsocketSessions().get(sessionKey);
                sendMessage(websocket, message);
            } else {
                throw new IllegalArgumentException("Failed to send message to Websocket session; session key not set.");
            }
        }
    }

    public WebsocketEndpoint getEndpoint() {
        return endpoint;
    }

    protected boolean isSendToAllSet(Message in) {
        Boolean value = in.getHeader(ConnectionConstants.SEND_TO_ALL, sendToAll, Boolean.class);
        return value == null ? false : value;
    }

    protected void sendToAll(Object message, Exchange exchange) throws Exception {
        Collection<Session> sessions = getEndpoint().getComponent().getWebsocketSessions().getAll();
        if (LOG.isLoggable(Level.FINE))
            LOG.fine("Sending to all sessions (" + sessions.size() + "): " + message);

        Exception exception = null;
        for (Session session : sessions) {
            try {
                sendMessage(session, message);
            } catch (Exception e) {
                if (exception == null) {
                    exception = new CamelExchangeException("Failed to deliver message to one or more recipients.", exchange, e);
                }
            }
        }
        if (exception != null) {
            throw exception;
        }
    }

    protected void sendMessage(Session session, Object message) throws IOException {
        if (session != null && session.isOpen()) {
            if (message instanceof String) {
                if (LOG.isLoggable(Level.FINE))
                    LOG.finer("Sending to session " + session.getId() + ": " + message);
                session.getBasicRemote().sendText((String) message);
            }
        }
    }
}
