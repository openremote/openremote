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

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
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
        session.setMaxIdleTimeout(WebsocketConstants.SESSION_MAX_IDLE_TIMEOUT_SECONDS * 1000);
        consumer.getEndpoint().getWebsocketSessions().add(session);
        this.consumer.sendMessage(session.getId(), getWebsocketAuth(session), null, exchange -> {
            exchange.getIn().setHeader(WebsocketConstants.SESSION, session);
            exchange.getIn().setHeader(WebsocketConstants.SESSION_OPEN, true);
        });
        session.addMessageHandler(String.class, message -> {
            if (LOG.isLoggable(Level.FINE))
                LOG.fine("Websocket session " + session.getId() + " message received: " + message);
            this.consumer.sendMessage(session.getId(), getWebsocketAuth(session), message, exchange -> {
                exchange.getIn().setHeader(WebsocketConstants.SESSION, session);
            });
        });
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        if (LOG.isLoggable(Level.FINE))
            LOG.fine("Websocket session close: " + session.getId() + " " + closeReason);
        this.consumer.sendMessage(session.getId(), getWebsocketAuth(session), closeReason, exchange -> {
            exchange.getIn().setHeader(WebsocketConstants.SESSION, session);
            exchange.getIn().setHeader(WebsocketConstants.SESSION_CLOSE, true);
        });
        consumer.getEndpoint().getWebsocketSessions().remove(session);
    }

    @Override
    public void onError(Session session, Throwable thr) {
        super.onError(session, thr);
        if (LOG.isLoggable(Level.INFO))
            LOG.log(Level.INFO, "Websocket session error: " + session.getId(), thr);
        this.consumer.sendMessage(session.getId(), getWebsocketAuth(session), thr, exchange -> {
            exchange.getIn().setHeader(WebsocketConstants.SESSION, session);
            exchange.getIn().setHeader(WebsocketConstants.SESSION_CLOSE_ERROR, true);
        });
        consumer.getEndpoint().getWebsocketSessions().remove(session);
    }

    protected WebsocketAuth getWebsocketAuth(Session session) {
        WebsocketAuth auth = (WebsocketAuth) session.getUserProperties().get(WebsocketConstants.AUTH);
        if (auth == null)
            throw new IllegalStateException("No authorization details in websocket session: " + session.getId());
        return auth;
    }
}
