package org.openremote.component.device;

import org.apache.camel.Endpoint;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultConsumer;

/**
 * This consumer takes gateways and dynamically creates/removes
 * routes from each gateway to the iot:device endpoint
 */
public class DeviceReadConsumer extends DefaultConsumer {
    public DeviceReadConsumer(Endpoint endpoint, Processor processor) {
        super(endpoint, processor);
    }


}
