package org.openremote.agent.device;

import org.apache.camel.Component;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;

public class DeviceEndpoint extends DefaultEndpoint {
    protected final DeviceService deviceService;

    public DeviceEndpoint(String endpointUri, Component component, DeviceService deviceService) {
        super(endpointUri, component);
        this.deviceService = deviceService;
    }

    @Override
    public Producer createProducer() throws Exception {
        if (getEndpointUri().toLowerCase().endsWith("inventory")) {
            return new DeviceInventoryProducer(this);
        }

        // TODO: Validate device against inventory
        return new DeviceWriteProducer(this);
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        // TODO: Validate device against inventory
        return new DeviceReadConsumer(this, processor);
    }

    @Override
    public boolean isSingleton() {
        return false;
    }
}
