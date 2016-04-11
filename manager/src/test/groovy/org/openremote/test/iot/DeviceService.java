package org.openremote.test.iot;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.message.MessageBrokerContext;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.manager.shared.device.Device;
import org.openremote.manager.shared.gateway.Gateway;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DeviceService implements ContainerService, org.openremote.manager.server.device.DeviceService {
    public static final String HEADER_GATEWAY_ID = "GatewayId";
    private static final Logger LOG = Logger.getLogger(DeviceService.class.getName());
    protected List<Gateway> gateways = new ArrayList<>();
    public ProducerTemplate messageProducerTemplate;
    protected MessageBrokerContext context;

    @Override
    public void init(Container container) throws Exception {
    }

    @Override
    public void configure(Container container) throws Exception {
        MessageBrokerService messageBrokerService = container.getService(MessageBrokerService.class);
        context = messageBrokerService.getContext();

        configure(context);

        messageProducerTemplate = context.createProducerTemplate();

        // TODO: Implement component interface for gateway service
//        context.addComponent("iot", new DeviceComponent(context));
    }

    @Override
    public void start(Container container) throws Exception {
        // TODO: Load in saved gateways and start them
    }

    @Override
    public void stop(Container container) throws Exception {
        // TODO: Stop all gateways
    }

    @Override
    public List<Gateway> getGateways() {
        return gateways;
    }

    @Override
    public void addGateway(Gateway gateway) {
        if(gateways.contains(gateway)) {
            return;
        }

        // TODO: Deal with concurrency issues
        int gatewayId = gateways.size();
        gateways.add(gatewayId, gateway);

        // Add routes for this gateway
        URI gatewayUri = URI.create(gateway.getEndpointUri());
        URI inventoryUri = gatewayUri.resolve(gatewayUri.getPath() + '/' + "inventory");
        URI discoveryUri = gatewayUri.resolve(gatewayUri.getPath() + '/' + "discovery");
        URI readUri = gatewayUri.resolve(gatewayUri.getPath() + '/' + "read");
        URI writeUri = gatewayUri.resolve(gatewayUri.getPath() + '/' + "write");

        try {
            context.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from(inventoryUri.toString())
                            .routeId("Gateway '" + gateway.getName() + "' devices CRUD")
                            .process(exchange -> {
                                exchange.getIn().setHeader(HEADER_GATEWAY_ID, gatewayId);
                            })
                            .to("device:inventory");
                }
            });
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error creating routes for gateway '" + gatewayUri + "'", e);
        }
    }

    @Override
    public void removeGateway(Gateway gateway) {

    }

    @Override
    public void stopGateway(Gateway gateway) {

    }

    @Override
    public void startGateway(Gateway gateway) {

    }

    @Override
    public int getGatewayId(Gateway gateway) {
        return 0;
    }

    @Override
    public List<Device> getDevices() {
        return null;
    }

    @Override
    public void addDevice(int gatewayId, Device device) {

    }

    @Override
    public void removeDevice(Device device) {

    }

    protected void configure(MessageBrokerContext context) {

    }
}
