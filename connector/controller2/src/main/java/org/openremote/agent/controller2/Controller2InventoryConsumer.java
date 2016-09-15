package org.openremote.agent.controller2;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.openremote.agent.controller2.model.DeviceListener;
import org.openremote.manager.shared.agent.InventoryModifiedEvent;
import org.openremote.manager.shared.asset.Asset;

import java.util.logging.Logger;

import static org.openremote.manager.shared.agent.InventoryModifiedEvent.Cause.*;

public class Controller2InventoryConsumer extends Controller2Consumer implements DeviceListener {

    private static final Logger LOG = Logger.getLogger(Controller2InventoryConsumer.class.getName());

    public Controller2InventoryConsumer(Controller2Endpoint endpoint, Processor processor) {
        super(endpoint, processor);
    }

    @Override
    synchronized protected void doStart() throws Exception {
        super.doStart();
        getEndpoint().getAdapter().getControllerState().addDeviceListener(this);
    }

    @Override
    synchronized protected void doStop() throws Exception {
        getEndpoint().getAdapter().getControllerState().removeDeviceListener(this);
        super.doStop();
    }

    @Override
    public void onDeviceAdded(Asset device) {
        if (!isStarted() && !isStarting()) {
            LOG.fine("Received device added event but consumer hasn't been started");
            return;
        }

        LOG.fine("Starting new exchange for added device:  " + device);
        Exchange exchange = getEndpoint().createExchange();
        exchange.getIn().setBody(new InventoryModifiedEvent(device, PUT));
        processExchange(exchange);
    }

    @Override
    public void onDeviceRemoved(Asset device) {
        if (!isStarted() && !isStarting()) {
            LOG.fine("Received device removed event but consumer hasn't been started");
            return;
        }

        LOG.fine("Starting new exchange for removed device: " + device);
        Exchange exchange = getEndpoint().createExchange();
        exchange.getIn().setBody(new InventoryModifiedEvent(device, DELETE));
        processExchange(exchange);
    }

    @Override
    public void onDeviceUpdated(Asset device) {
        if (!isStarted() && !isStarting()) {
            LOG.fine("Received device updated event but consumer hasn't been started");
            return;
        }

        LOG.fine("Starting new exchange for updated device: " + device);
        Exchange exchange = getEndpoint().createExchange();
        exchange.getIn().setBody(new InventoryModifiedEvent(device, PUT));
        processExchange(exchange);
    }

    protected void processExchange(Exchange exchange) {
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
