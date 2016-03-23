package org.openremote.test.controller2;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.message.MessageBrokerContext;
import org.openremote.container.message.MessageBrokerService;

import java.util.ArrayList;
import java.util.List;

// TODO This is an example service for testing, replaced later with production code
public class Controller2Service implements ContainerService {

    public List<String> discoveredItems = new ArrayList<>();
    public List<String> receivedSensorData = new ArrayList<>();
    public ProducerTemplate messageProducerTemplate;

    @Override
    public void init(Container container) throws Exception {
    }

    @Override
    public void configure(Container container) throws Exception {
        MessageBrokerService messageBrokerService = container.getService(MessageBrokerService.class);
        MessageBrokerContext context = messageBrokerService.getContext();

        configure(context);

        messageProducerTemplate = context.createProducerTemplate();

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {

                from("direct:triggerDiscovery")
                    .routeId("Trigger discovery")
                    .to("mockController2://10.0.0.123/discovery");

                from("mockController2://10.0.0.123/discovery")
                    .routeId("Discovered devices")
                    .process(exchange -> {
                        List<String> discovered = exchange.getIn().getBody(List.class);
                        discoveredItems.addAll(discovered);
                    });

                from("mockController2://10.0.0.123")
                    .routeId("Received sensor values")
                    .process(exchange -> {
                        receivedSensorData.add(exchange.getIn().getBody(String.class));
                    });

                from("direct:sendCommand")
                    .routeId("Send command")
                    .to("mockController2://10.0.0.123");
            }
        });
    }

    protected void configure(MessageBrokerContext context) {

    }

    @Override
    public void start(Container container) {
    }

    @Override
    public void stop(Container container) {
    }

}