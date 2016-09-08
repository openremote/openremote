package org.openremote.test.controller2;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.openremote.agent.controller2.Controller2Component;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.message.MessageBrokerContext;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.manager.shared.device.Device;

import java.util.ArrayList;
import java.util.List;

import static org.openremote.manager.shared.connector.ConnectorComponent.*;

// TODO This is an example service for testing, replaced later with production code
public class Controller2Service implements ContainerService {

    public List<Device> addedDevices = new ArrayList<>();
    public List<Device> removedDevices = new ArrayList<>();
    public List<Device> updatedDevices = new ArrayList<>();
    public ProducerTemplate messageProducerTemplate;

    @Override
    public void init(Container container) throws Exception {
    }

    @Override
    public void configure(Container container) throws Exception {
        MessageBrokerService messageBrokerService = container.getService(MessageBrokerService.class);
        MessageBrokerContext context = messageBrokerService.getContext();
        messageProducerTemplate = context.createProducerTemplate();

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("controller2://192.168.99.100:8083/inventory")
                        .routeId("Device Inventory")
                        .choice()
                            .when(header(HEADER_DEVICE_ACTION).isEqualTo(ACTION_CREATE))
                                .process(exchange -> {
                                    Device device = exchange.getIn().getBody(Device.class);
                                    addedDevices.add(device);
                            }).endChoice()
                            .when(header(HEADER_DEVICE_ACTION).isEqualTo(ACTION_DELETE))
                            .process(exchange -> {
                                Device device = exchange.getIn().getBody(Device.class);
                                removedDevices.add(device);
                            }).endChoice()
                            .when(header(HEADER_DEVICE_ACTION).isEqualTo(ACTION_UPDATE))
                            .process(exchange -> {
                                Device device = exchange.getIn().getBody(Device.class);
                                updatedDevices.add(device);
                            }).end();

                from("direct:triggerDiscovery")
                    .routeId("Trigger discovery")
                    .to("controller2://192.168.99.100:8083/discovery");
            }
        });
    }

    @Override
    public void start(Container container) {
    }

    @Override
    public void stop(Container container) {
    }

    public void triggerDiscovery() {
        messageProducerTemplate.sendBody("direct:triggerDiscovery", "");
    }

}