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

import org.openremote.container.security.AuthContext;
import org.openremote.container.web.ConnectionConstants;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import java.io.IOException;
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
        // TODO We never expire idle websocket sessions, the assumption is that only authenticate clients can
        // open a session and if their SSO (managed by Keycloak) expires, they are logged out
        session.setMaxIdleTimeout(0);
        consumer.getEndpoint().getWebsocketSessions().add(session);
        this.consumer.sendMessage(session.getId(), getHandshakeAuth(session), null, exchange -> {
            exchange.getIn().setHeader(ConnectionConstants.SESSION, session);
            exchange.getIn().setHeader(ConnectionConstants.SESSION_OPEN, true);
        });
        session.addMessageHandler(String.class, message -> {
            if (LOG.isLoggable(Level.FINE))
                LOG.fine("Websocket session " + session.getId() + " message received: " + message);
            this.consumer.sendMessage(session.getId(), getHandshakeAuth(session), message, exchange -> {
                exchange.getIn().setHeader(ConnectionConstants.SESSION, session);
            });
        });
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        if (LOG.isLoggable(Level.FINE))
            LOG.fine("Websocket session close: " + session.getId() + " " + closeReason);
        this.consumer.sendMessage(session.getId(), getHandshakeAuth(session), closeReason, exchange -> {
            exchange.getIn().setHeader(ConnectionConstants.SESSION, session);
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
        this.consumer.sendMessage(session.getId(), getHandshakeAuth(session), thr, exchange -> {
            exchange.getIn().setHeader(ConnectionConstants.SESSION, session);
            exchange.getIn().setHeader(ConnectionConstants.SESSION_CLOSE_ERROR, true);
        });
        consumer.getEndpoint().getWebsocketSessions().remove(session);
    }

    protected AuthContext getHandshakeAuth(Session session) {
        AuthContext auth = (AuthContext) session.getUserProperties().get(ConnectionConstants.HANDSHAKE_AUTH);
        if (auth == null)
            throw new IllegalStateException("No authorization details in websocket session: " + session.getId());
        return auth;
    }
}
