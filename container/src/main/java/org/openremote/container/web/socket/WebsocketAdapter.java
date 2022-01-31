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

import org.apache.camel.Exchange;
import org.openremote.container.security.AuthContext;
import org.openremote.container.web.ConnectionConstants;
import org.openremote.model.Constants;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import java.io.IOException;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebsocketAdapter extends Endpoint {

    private static final Logger LOG = Logger.getLogger(WebsocketAdapter.class.getName());

    final protected WebsocketConsumer consumer;

    public WebsocketAdapter(WebsocketConsumer consumer) {
        this.consumer = consumer;
    }

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        if (LOG.isLoggable(Level.FINE))
            LOG.fine("Websocket session open: " + session.getId());
        // TODO We never expire idle websocket sessions, the assumption is that only authenticated clients can
        // open a session and if their SSO (managed by Keycloak) expires, they are logged out
        session.setMaxIdleTimeout(0);
        consumer.getEndpoint().getWebsocketSessions().add(session);
        this.consumer.sendMessage(null, exchange -> {
            this.prepareExchange(exchange, session);
            exchange.getIn().setHeader(ConnectionConstants.SESSION_OPEN, true);
        });
        session.addMessageHandler(String.class, message -> {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Websocket session " + session.getId() + " message received: " + message);
            }
            this.consumer.sendMessage(message, exchange -> this.prepareExchange(exchange, session));
        });
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Websocket session close: " + session.getId() + " " + closeReason);
        }
        this.consumer.sendMessage(closeReason, exchange -> {
            this.prepareExchange(exchange, session);
            exchange.getIn().setHeader(ConnectionConstants.SESSION_CLOSE, true);
        });
        consumer.getEndpoint().getWebsocketSessions().remove(session);
    }

    @Override
    public void onError(Session session, Throwable thr) {
        super.onError(session, thr);
        
        // Ignore connection reset
        if (!(thr instanceof IOException && thr.getMessage().equals("Connection reset by peer"))) {
            if (LOG.isLoggable(Level.INFO))
                LOG.log(Level.INFO, "Websocket session error: " + session.getId(), thr);
        }
        this.consumer.sendMessage(thr, exchange -> {
            this.prepareExchange(exchange, session);
            exchange.getIn().setHeader(ConnectionConstants.SESSION_CLOSE_ERROR, true);
        });
        consumer.getEndpoint().getWebsocketSessions().remove(session);
    }

    protected void prepareExchange(Exchange exchange, Session session) {
        exchange.getIn().setHeader(ConnectionConstants.SESSION_KEY, session.getId());
        exchange.getIn().setHeader(Constants.AUTH_CONTEXT, getHandshakeAuth(session));
        exchange.getIn().setHeader(Constants.REALM_PARAM_NAME, getHandshakeRealm(session));
        exchange.getIn().setHeader(ConnectionConstants.SESSION_TERMINATOR, getSessionTerminator(session));
    }

    protected AuthContext getHandshakeAuth(Session session) {
        return (AuthContext) session.getUserProperties().get(ConnectionConstants.HANDSHAKE_AUTH);
    }

    protected String getHandshakeRealm(Session session) {
        return (String) session.getUserProperties().get(ConnectionConstants.HANDSHAKE_REALM);
    }

    protected Runnable getSessionTerminator(Session session) {
        return () -> {
            try {
                session.close();
            } catch (RejectedExecutionException ignored) {
            } catch (IOException ignored) {
                LOG.log(Level.INFO, "Failed to close client session: " + session.getId());
            }
        };
    }
}
