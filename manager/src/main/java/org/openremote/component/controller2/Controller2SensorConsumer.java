package org.openremote.component.controller2;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.util.logging.Logger;

public class Controller2SensorConsumer extends Controller2Consumer implements Controller2Adapter.SensorListener{

    private static final Logger LOG = Logger.getLogger(Controller2SensorConsumer.class.getName());

    public Controller2SensorConsumer(Controller2Endpoint endpoint, Processor processor) {
        super(endpoint, processor);
    }

    @Override
    synchronized protected void doStart() throws Exception {
        super.doStart();
        getEndpoint().getAdapter().addSensorListener(this);
    }

    @Override
    synchronized protected void doStop() throws Exception {
        getEndpoint().getAdapter().removeSensorListener(this);
        super.doStop();
    }

    @Override
    public void onUpdate(String state) {
        LOG.fine("Consuming state change: " + state);
        Exchange exchange = getEndpoint().createExchange();
        exchange.getIn().setBody(state);
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
