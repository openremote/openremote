package org.openremote.component.controller2;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;

import java.util.logging.Logger;


public class Controller2ReadProducer extends DefaultProducer {

    private static final Logger LOG = Logger.getLogger(Controller2ReadProducer.class.getName());

    public Controller2ReadProducer(Endpoint endpoint) {
        super(endpoint);
    }

    @Override
    public Controller2Endpoint getEndpoint() {
        return (Controller2Endpoint) super.getEndpoint();
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        // TODO: Implement read producer (should support inOut response for ad-hoc resource reading)
    }
}
