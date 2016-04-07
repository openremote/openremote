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

@UriEndpoint(
    scheme = "controller2",
    title = "OpenRemote Controller v2 Adapter",
    syntax = "controller2://<IP or host name>:<port>[/discovery]",
    consumerClass = Controller2Consumer.class
)
public class Controller2Endpoint extends DefaultEndpoint {

    final protected Controller2Adapter.Manager adapterManager;
    final protected boolean discoveryOnly;
    final protected String host;
    final protected int port;
    final protected String hostPortKey; // TODO: For each host:port combination, we'll manage one Controller2Adapter

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
                               String host, int port, boolean discoveryOnly) {
        super(endpointUri, component);
        this.adapterManager = adapterManager;
        this.host = host;
        this.port = port;
        this.hostPortKey = host + ":" + port;
        this.discoveryOnly = discoveryOnly;
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
        return discoveryOnly
        ? new Controller2DiscoveryProducer(this)
        : new Controller2ActuatorProducer(this);
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        return discoveryOnly
            ? new Controller2DiscoveryConsumer(this, processor)
            : new Controller2SensorConsumer(this, processor);
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    public Controller2Adapter getAdapter() {
        if (adapter == null) {
            adapter = adapterManager.openAdapter(hostPortKey); // TODO If you want more options passed into the adapter, see @UriParam example above
            if (adapter == null)
                throw new IllegalStateException("Manager did not open adapter: " + hostPortKey);
        }
        return adapter;
    }

    public void sendCommand(String command, String argument) {
        getAdapter().sendCommand(command, argument); // TODO: You can enrich the call here with some other endpoint settings
    }

}
