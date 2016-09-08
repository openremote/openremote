package org.openremote.test.controller2

import org.openremote.test.ContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

class Controller2Test extends Specification implements ContainerTrait {

    def "Get Device Inventory"() {
        given: "a test controller service"
        def controllerService = new Controller2Service()

        and: "a clean result state"
        def conditions = new PollingConditions(timeout: 3)

        when: "the server container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices(controllerService))

        and: "discovery is triggered"
        controllerService.triggerDiscovery()

        then: "the service should receive the devices from the inventory"
        conditions.eventually {
            assert controllerService.addedDevices.size() > 0
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

    /* TODO broken test
    def "Execute discovery"() {
        given: "Some fake devices"
        def deviceA = "DeviceAAA"
        def deviceB = "DeviceBBB"

        and: "a spy of the controller adapter returning fake devices"
        def controllerAdapter = Spy(Controller2Adapter, constructorArgs: [new URL("http://10.0.0.123:8080"), null, null])
        controllerAdapter.triggerDiscovery() >> {
            mockObject.instance.discoveryListeners.each {
                it.onDiscovery([deviceA, deviceB])
            }
        }

        and: "a mock controller component that uses the testing adapter"
        def controllerService = new Controller2Service() {
            // The "controller2://..." component will be automatically discovered, however,
            // for testing we use "mockController2://..." endpoints and a mock adapter
            @Override
            protected void configure(MessageBrokerContext context) {

                Controller2Adapter.Manager mockManager = new Controller2Adapter.Manager() {
                    @Override
                    Controller2Adapter openAdapter(URL url, String username, String password) {
                        assert url == new URL("http://10.0.0.123:8080/controller")
                        return controllerAdapter;
                    }

                    @Override
                    void closeAdapter(Controller2Adapter adapter) {
                        // Don't care
                    }
                }

                context.addComponent("mockController2", new Controller2Component(mockManager))
            }
        }

        and: "a server container that runs the service"
        def serverPort = findEphemeralPort();
        def container = startContainer(defaultConfig(serverPort), defaultServices(controllerService))

        and: "a clean result state"
        def conditions = new PollingConditions(timeout: 5)

        when: "discovery is triggered with a camel message"
        controllerService.messageProducerTemplate.sendBody("direct:triggerDiscovery", "") // Empty message is fine

        then: "the service should receive the discovered items"
        conditions.eventually {
            assert controllerService.discoveredItems == [deviceA, deviceB]
        }

        and: "the server should be stopped"
        stopContainer(container);
    }

    def "Receive sensor values"() {
        given: "Some sensor values"
        def valueA = "AAA"
        def valueB = "BBB"
        def valueC = "CCC"

        and: "a spy of the controller adapter"
        def controllerAdapter = Spy(Controller2Adapter, constructorArgs: [new URL("http://10.0.0.123:8080"), null, null])

        and: "a mock controller component"
        def controllerService = new Controller2Service() {
            // The "controller2://..." component will be automatically discovered, however,
            // for testing we use "mockController2://..." endpoints and a mock adapter
            @Override
            protected void configure(MessageBrokerContext context) {

                Controller2Adapter.Manager mockManager = new Controller2Adapter.Manager() {
                    @Override
                    Controller2Adapter openAdapter(URL url, String username, String password) {
                        assert url == new URL("http://10.0.0.123:8080/controller")
                        return controllerAdapter;
                    }

                    @Override
                    void closeAdapter(Controller2Adapter adapter) {
                        // Don't care
                    }
                }

                context.addComponent("mockController2", new Controller2Component(mockManager))
            }
        }

        and: "a server container that runs the service"
        def serverPort = findEphemeralPort();
        def container = startContainer(defaultConfig(serverPort), defaultServices(controllerService))

        and: "a clean result state"
        def conditions = new PollingConditions(timeout: 5)

        when: "some sensor values are pushed into the adapter"
        controllerAdapter.sensorListeners.each {
            it.onUpdate(valueA);
            it.onUpdate(valueB);
            it.onUpdate(valueC);
        }

        then: "the service should receive the sensor values"
        conditions.eventually {
            assert controllerService.receivedSensorData == [valueA, valueB, valueC]
        }

        and: "the server should be stopped"
        stopContainer(container);
    }

    def "Send commands"() {
        given: "A command with arguments"
        def command = "foo"
        def arg = "bar"

        and: "an asynchronous result"
        def result = new BlockingVariables(5)

        and: "a spy of the controller adapter that captures the result"
        def controllerAdapter = Spy(Controller2Adapter, constructorArgs: [new URL("http://10.0.0.123:8080"), null, null])
        controllerAdapter.sendCommand(_, _) >> { String c, String a ->
            result.command = c;
            result.arg = a;
        }

        and: "a mock controller component"
        def controllerService = new Controller2Service() {
            // The "controller2://..." component will be automatically discovered, however,
            // for testing we use "mockController2://..." endpoints and a mock adapter
            @Override
            protected void configure(MessageBrokerContext context) {

                Controller2Adapter.Manager mockManager = new Controller2Adapter.Manager() {
                    @Override
                    Controller2Adapter openAdapter(URL url, String username, String password) {
                        assert url == new URL("http://10.0.0.123:8080/controller")
                        return controllerAdapter;
                    }

                    @Override
                    void closeAdapter(Controller2Adapter adapter) {
                        // Don't care
                    }
                }

                context.addComponent("mockController2", new Controller2Component(mockManager))
            }
        }

        and: "a server container that runs the service"
        def serverPort = findEphemeralPort();
        def container = startContainer(defaultConfig(serverPort), defaultServices(controllerService))

        when: "a command message is send"
        controllerService.messageProducerTemplate.sendBodyAndHeader(
                "direct:write",
                arg,
                Controller2Component.HEADER_COMMAND_VALUE, command
        )

        then: "the adapter spy should receive the command"
        result.command == command;
        result.arg == arg;

        and: "the server should be stopped"
        stopContainer(container);
    }
    */
}
