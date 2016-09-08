package org.openremote.agent.controller2;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;
import org.apache.camel.util.URISupport;

import java.util.logging.Level;
import java.util.logging.Logger;


public class Controller2WriteProducer extends DefaultProducer {

    private static final Logger LOG = Logger.getLogger(Controller2WriteProducer.class.getName());

    protected String deviceUri;
    protected String resourceUri;

    public Controller2WriteProducer(Endpoint endpoint, String deviceUri, String resourceUri) {
        super(endpoint);
        this.deviceUri = deviceUri;
        this.resourceUri = resourceUri;
    }

    @Override
    public Controller2Endpoint getEndpoint() {
        return (Controller2Endpoint) super.getEndpoint();
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        // Extract write related headers
        String deviceUri = exchange.getIn().getHeader(
            Controller2Component.HEADER_DEVICE_URI,
            this.deviceUri,
            String.class
        );

        String resourceUri = exchange.getIn().getHeader(
            Controller2Component.HEADER_RESOURCE_URI,
            this.resourceUri,
            String.class
        );

        Object commandValue = exchange.getIn().getBody();

        if (deviceUri == null || "".equals(deviceUri) || resourceUri == null || "".equals(resourceUri)) {
            throw new IllegalArgumentException(
                "Both device and resource URI message headers must be defined for write producer"
            );
        }

        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Writing to '" + deviceUri + " : " + resourceUri + "' with value: " + commandValue);
        }

        getEndpoint().getAdapter().getControllerState().writeResource(deviceUri, resourceUri, commandValue);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + URISupport.sanitizeUri(getEndpoint().getEndpointUri()) + "]";
    }
}
