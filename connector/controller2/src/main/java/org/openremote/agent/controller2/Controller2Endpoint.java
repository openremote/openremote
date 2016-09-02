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
package org.openremote.agent.controller2;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;

import java.net.MalformedURLException;
import java.net.URL;

@UriEndpoint(
    scheme = "controller2",
    title = "OpenRemote Controller 2.x",
    syntax = Controller2Component.URI_SYNTAX,
    consumerClass = Controller2Consumer.class
)
public class Controller2Endpoint extends DefaultEndpoint {

    final protected Controller2Adapter.Manager adapterManager;
    final protected boolean isDiscovery;
    final protected boolean isInventory;

    @UriParam
    protected String host;

    @UriParam
    protected Integer port;

    @UriParam
    protected String username;

    @UriParam
    protected String password;

    protected String deviceUri;
    protected String resourceUri;

    protected Controller2Adapter adapter;

    public Controller2Endpoint(String endpointUri, Controller2Component component, Controller2Adapter.Manager adapterManager, String path) {
        super(endpointUri, component);

        this.adapterManager = adapterManager;
        this.isDiscovery = "/discovery".equals(path);
        this.isInventory = "/inventory".equals(path);

        if (!isDiscovery && !isInventory && path != null) {
            String[] deviceResourceArr = path.split("//");
            if (deviceResourceArr.length == 2) {
                deviceUri = deviceResourceArr[0];
                resourceUri = deviceResourceArr[1];
            }
        }
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public Controller2Component getComponent() {
        return (Controller2Component) super.getComponent();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        if (adapter != null)
            adapterManager.closeAdapter(adapter);
    }

    @Override
    public Producer createProducer() throws Exception {
        if (isDiscovery) {
            return new Controller2DiscoveryProducer(this);
        }
        return new Controller2WriteProducer(this, deviceUri, resourceUri);
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        if (isDiscovery) {
            return new Controller2DiscoveryConsumer(this, processor);
        } else if (isInventory) {
            return new Controller2InventoryConsumer(this, processor);
        }

        // Read consumer needs specific device and resource so gateway can
        // deal with providing push notifications whichever way it needs to
        if (deviceUri == null || resourceUri == null) {
            return new Controller2ReadConsumer(this, processor, deviceUri, resourceUri);
        } else {
            throw new UnsupportedOperationException("Read consumer requires deviceURI and resource URI");
        }
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    public Controller2Adapter getAdapter() {
        if (adapter == null) {
            try {
                // TODO: HTTPS support should be implemented with a "?secure=true|false" Endpoint URL query param
                URL controllerUrl = new URL("http", getHost(), getPort(), "/controller");
                adapter = adapterManager.openAdapter(controllerUrl, username, password);
                if (adapter == null)
                    throw new IllegalStateException("Manager did not open adapter: " + controllerUrl);
            } catch (MalformedURLException ex) {
                throw new RuntimeException("Invalid controller URL: " + ex);
            }
        }
        return adapter;
    }
}
