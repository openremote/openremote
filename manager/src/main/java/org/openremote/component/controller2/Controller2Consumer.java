package org.openremote.component.controller2;

import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultConsumer;

public class Controller2Consumer extends DefaultConsumer {

    public Controller2Consumer(Controller2Endpoint endpoint, Processor processor) {
        super(endpoint, processor);
    }

    @Override
    public Controller2Endpoint getEndpoint() {
        return (Controller2Endpoint) super.getEndpoint();
    }
}
