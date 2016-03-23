package org.openremote.component.controller2;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;

public class Controller2DiscoveryProducer extends DefaultProducer {

    public Controller2DiscoveryProducer(Endpoint endpoint) {
        super(endpoint);
    }

    @Override
    public Controller2Endpoint getEndpoint() {
        return (Controller2Endpoint) super.getEndpoint();
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        getEndpoint().getAdapter().triggerDiscovery();
    }
}
