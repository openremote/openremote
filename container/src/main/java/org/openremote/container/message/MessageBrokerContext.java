package org.openremote.container.message;

import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.PropertyPlaceholderDelegateRegistry;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.spi.Registry;

public class MessageBrokerContext extends DefaultCamelContext {

    @Override
    protected Registry createRegistry() {
        return new SimpleRegistry();
    }

    @Override
    public SimpleRegistry getRegistry() {
        return (SimpleRegistry) ((PropertyPlaceholderDelegateRegistry) super.getRegistry()).getRegistry();
    }
}
