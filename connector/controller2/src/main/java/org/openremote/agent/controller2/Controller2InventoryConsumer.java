package org.openremote.agent.controller2;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.openremote.agent.controller2.model.DeviceListener;
import org.openremote.manager.shared.device.Device;

import java.util.logging.Logger;

import static org.openremote.manager.shared.connector.ConnectorComponent.ACTION_CREATE;
import static org.openremote.manager.shared.connector.ConnectorComponent.ACTION_DELETE;
import static org.openremote.manager.shared.connector.ConnectorComponent.ACTION_UPDATE;
import static org.openremote.manager.shared.connector.ConnectorComponent.HEADER_DEVICE_ACTION;

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
    public void onDeviceAdded(Device device) {
        if (!isStarted() && !isStarting()) {
            LOG.fine("Received device added event but consumer hasn't been started");
            return;
        }

        LOG.fine("Starting new exchange for added device '" + device + "'");
        Exchange exchange = getEndpoint().createExchange();
        exchange.getIn().setHeader(HEADER_DEVICE_ACTION, ACTION_CREATE);
        exchange.getIn().setBody(device);
        processExchange(exchange);
    }

    @Override
    public void onDeviceRemoved(Device device) {
        if (!isStarted() && !isStarting()) {
            LOG.fine("Received device removed event but consumer hasn't been started");
            return;
        }

        LOG.fine("Starting new exchange for removed device '" + device.getUri() + "'");
        Exchange exchange = getEndpoint().createExchange();
        exchange.getIn().setHeader(HEADER_DEVICE_ACTION, ACTION_DELETE);
        exchange.getIn().setBody(device);
        processExchange(exchange);
    }

    @Override
    public void onDeviceUpdated(Device device) {
        if (!isStarted() && !isStarting()) {
            LOG.fine("Received device updated event but consumer hasn't been started");
            return;
        }

        LOG.fine("Starting new exchange for updated device '" + device.getUri() + "'");
        Exchange exchange = getEndpoint().createExchange();
        exchange.getIn().setHeader(HEADER_DEVICE_ACTION, ACTION_UPDATE);
        exchange.getIn().setBody(device);
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
