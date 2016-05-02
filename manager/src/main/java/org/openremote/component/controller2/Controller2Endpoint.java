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
package org.openremote.component.controller2;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.spi.UriEndpoint;
import org.jgroups.annotations.Unsupported;

import java.net.URL;
import java.util.logging.Logger;

@UriEndpoint(
    scheme = "controller2",
    title = "OpenRemote Controller v2 Adapter",
    syntax = "controller2:<scheme>://<IP or host name>:<port>[/discovery]",
    consumerClass = Controller2Consumer.class
)
public class Controller2Endpoint extends DefaultEndpoint {
    private static final Logger LOG = Logger.getLogger(Controller2Endpoint.class.getName());
    final protected Controller2Adapter.Manager adapterManager;
    final protected boolean isDiscovery;
    final protected boolean isInventory;
    final protected URL controllerUrl;
    protected String username;
    protected String password;
    protected String deviceUri;
    protected String resourceUri;

    protected Controller2Adapter adapter;

    /* TODO This is how you do options. A query parameter on the endpoint URI is automatically a property of the endpoint.
    @org.apache.camel.spi.UriParam(
        label = "Foo",
        description = "This will be automatically parsed from the endpoint URI query parameters!",
        defaultValue = "bar"
    )
    protected String foo;

    TODO Also should add getter/setter for this property
    */

    public Controller2Endpoint(String endpointUri, Controller2Component component, Controller2Adapter.Manager adapterManager,
                               URL controllerUrl, String path) {
        this(endpointUri, component, adapterManager, controllerUrl, null, null, path);
    }

    public Controller2Endpoint(String endpointUri, Controller2Component component, Controller2Adapter.Manager adapterManager,
                               URL controllerUrl, String username, String password, String path) {
        super(endpointUri, component);
        this.adapterManager = adapterManager;
        this.controllerUrl = controllerUrl;
        this.username = username;
        this.password = password;
        this.isDiscovery = "/discovery".equals(path);
        this.isInventory = "/inventory".equals(path);

        if (!isDiscovery && !isInventory && path != null) {
            // See if we have device and resource URIs
            // There must be a nicer way of dealing with this??
            // maybe we should use params and let setProperties on component resolve the values
            String[] deviceResourceArr = path.split("//");
            if (deviceResourceArr.length == 2) {
                deviceUri = deviceResourceArr[0];
                resourceUri = deviceResourceArr[1];
            }
        }
    }

    public void setAuthUsername(String username) {
        this.username = username;
    }

    public void setAuthPassword(String password) {
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
            adapter = adapterManager.openAdapter(controllerUrl, username, password); // TODO If you want more options passed into the adapter, see @UriParam example above

            if (adapter == null)
                throw new IllegalStateException("Manager did not open adapter: " + controllerUrl.toString());
        }
        return adapter;
    }
}
