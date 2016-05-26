package org.openremote.agent.device;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;

public class DeviceWriteProducer extends DefaultProducer {
    public DeviceWriteProducer(Endpoint endpoint) {
        super(endpoint);
    }

    @Override
    public void process(Exchange exchange) throws Exception {

    }
}
