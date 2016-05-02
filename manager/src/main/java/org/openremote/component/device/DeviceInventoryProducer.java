package org.openremote.component.device;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;

public class DeviceInventoryProducer extends DefaultProducer {
    public DeviceInventoryProducer(Endpoint endpoint) {
        super(endpoint);
    }

    @Override
    public void process(Exchange exchange) throws Exception {

    }
}
