package org.openremote.component.controller2;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.util.List;
import java.util.logging.Logger;

public class Controller2DiscoveryConsumer extends Controller2Consumer implements Controller2Adapter.DiscoveryListener{

    private static final Logger LOG = Logger.getLogger(Controller2DiscoveryConsumer.class.getName());

    public Controller2DiscoveryConsumer(Controller2Endpoint endpoint, Processor processor) {
        super(endpoint, processor);
    }

    @Override
    synchronized protected void doStart() throws Exception {
        super.doStart();
        getEndpoint().getAdapter().addDiscoveryListener(this);
    }

    @Override
    synchronized protected void doStop() throws Exception {
        getEndpoint().getAdapter().removeDiscoveryListener(this);
        super.doStop();
    }

    @Override
    public void onDiscovery(List<String> list) {
        if (!isStarted()) {
            LOG.fine("Received discovery event but consumer hasn't been started");
            return;
        }
        LOG.fine("Starting new exchange for discovered devices: " + list.size());
        Exchange exchange = getEndpoint().createExchange();
        exchange.getIn().setBody(list);
        try {
            getProcessor().process(exchange);
        } catch (Exception ex) {
            getExceptionHandler().handleException("Error processing exchange", exchange, ex);
        } finally {
            if (exchange.getException() != null) {
                getExceptionHandler().handleException("Error processing exchange", exchange, exchange.getException());
            }
        }
    }

}
