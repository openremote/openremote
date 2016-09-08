package org.openremote.agent.controller2;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;
import org.apache.camel.util.URISupport;

import java.util.logging.Logger;


public class Controller2InventoryProducer extends DefaultProducer {

    private static final Logger LOG = Logger.getLogger(Controller2InventoryProducer.class.getName());

    public Controller2InventoryProducer(Endpoint endpoint) {
        super(endpoint);
    }

    @Override
    public Controller2Endpoint getEndpoint() {
        return (Controller2Endpoint) super.getEndpoint();
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        // TODO: Implement inventory producer (should support add, remove, update)
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + URISupport.sanitizeUri(getEndpoint().getEndpointUri()) + "]";
    }
}
