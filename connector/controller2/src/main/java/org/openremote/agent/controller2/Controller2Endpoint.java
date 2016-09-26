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
import org.openremote.manager.shared.connector.ConnectorComponent;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

public class Controller2Endpoint extends DefaultEndpoint {

    final protected Controller2AdapterManager adapterManager;
    final protected ConnectorComponent.Capability capability;

    protected String host;
    protected Integer port;
    protected String username;
    protected String password;
    protected Boolean secure;

    protected String deviceKey;

    protected Controller2Adapter adapter;

    public Controller2Endpoint(String endpointUri, Controller2Component component, Controller2AdapterManager adapterManager, Path path) {
        super(endpointUri, component);
        this.adapterManager = adapterManager;

        if (path.getNameCount() <= 0) {
            throw new IllegalArgumentException(
                "Desired endpoint capability missing from endpoint URI path: " + endpointUri
            );
        }

        capability = ConnectorComponent.Capability.valueOf(path.getName(0).toString());

        if (capability == ConnectorComponent.Capability.listen) {
            if (path.getNameCount() < 2 || (deviceKey = path.getName(1).toString()).length() == 0) {
                throw new IllegalArgumentException(
                    "Listen capability requires /" + ConnectorComponent.Capability.listen.name() + "/<deviceKey> in endpoint URI path: " + endpointUri
                );
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

    public Boolean getSecure() {
        return secure;
    }

    public void setSecure(Boolean secure) {
        this.secure = secure;
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
        switch(capability) {
            case discovery:
                return new Controller2DiscoveryProducer(this);
            case inventory:
                return new Controller2InventoryProducer(this);
            case read:
                return new Controller2ReadProducer(this);
            case write:
                return new Controller2WriteProducer(this);
        }
        throw new IllegalStateException("Unsupported producer capability: " + capability);
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        switch(capability) {
            case inventory:
                return new Controller2InventoryConsumer(this, processor);
            case listen:
                return new Controller2ListenConsumer(this, processor, deviceKey);
        }
        throw new IllegalStateException("Unsupported consumer capability: " + capability);
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    public Controller2Adapter getAdapter() {
        if (adapter == null) {
            try {
                String scheme = secure != null && secure ? "https" : "http";
                URL controllerUrl = new URL(scheme, getHost(), getPort(), "/controller");
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
