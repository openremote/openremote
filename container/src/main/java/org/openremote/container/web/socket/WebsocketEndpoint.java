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

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.apache.camel.util.ObjectHelper;

import java.util.Map;

@UriEndpoint(scheme = "ws", title = "Undertow Websocket", syntax = "ws:resourceUri", consumerClass = WebsocketConsumer.class, label = "http,websocket")
public class WebsocketEndpoint extends DefaultEndpoint {

    private WebsocketComponent component;

    @UriPath
    @Metadata(required = "true")
    private String resourceUri;

    @UriParam
    private Boolean sendToAll;

    public WebsocketEndpoint(WebsocketComponent component, String uri, String resourceUri, Map<String, Object> parameters) {
        super(uri, component);
        this.resourceUri = resourceUri;
        this.component = component;
    }

    @Override
    public WebsocketComponent getComponent() {
        ObjectHelper.notNull(component, "component");
        return (WebsocketComponent) super.getComponent();
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        ObjectHelper.notNull(component, "component");
        WebsocketConsumer consumer = new WebsocketConsumer(this, processor);
        configureConsumer(consumer);
        return consumer;
    }

    @Override
    public Producer createProducer() throws Exception {
        return new WebsocketProducer(this);
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public WebsocketSessions getWebsocketSessions() {
        return getComponent().getWebsocketSessions();
    }

    public void connect(WebsocketConsumer consumer) throws Exception {
        component.connect(consumer);
    }

    public void disconnect(WebsocketConsumer consumer) throws Exception {
        component.disconnect(consumer);
    }

    public Boolean getSendToAll() {
        return sendToAll;
    }

    public void setSendToAll(Boolean sendToAll) {
        this.sendToAll = sendToAll;
    }

    public String getResourceUri() {
        return resourceUri;
    }

    public void setResourceUri(String resourceUri) {
        this.resourceUri = resourceUri;
    }
}
