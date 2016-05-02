package org.openremote.test.controller2

import org.openremote.component.controller2.Controller2Adapter
import org.openremote.component.controller2.Controller2Component
import org.openremote.console.controller.Controller
import org.openremote.console.controller.connector.AsyncHttpConnector
import org.openremote.console.controller.connector.HttpConnector
import org.openremote.console.controller.connector.HttpConnector.ControllerCallback
import org.openremote.container.message.MessageBrokerContext
import org.openremote.entities.controller.ControllerInfo
import org.openremote.test.ManualContainerTrait
import spock.lang.Specification
import spock.util.concurrent.BlockingVariables
import spock.util.concurrent.PollingConditions

import java.nio.file.Files

class Controller2Test extends Specification implements ManualContainerTrait {

    def "Get Device Inventory"() {
        given: "A spy of the controller connector returning fake responses"
            def controllerConnector = Spy(AsyncHttpConnector)
            controllerConnector.doRequest(_,_,_,_,_) >> {
            URI uri, Map<String, String> headers, String content, ControllerCallback callback, Integer timeout ->
                switch (callback.getCommand()) {
                    case HttpConnector.RestCommand.CONNECT:
                        controllerConnector.handleResponse(callback, 200, null, null)
                        break;
                    case HttpConnector.RestCommand.GET_XML:
                        byte[] xml = Files.readAllBytes(java.nio.file.Paths.get("src/test/groovy/org/openremote/test/controller2/controller.xml"));
                        controllerConnector.handleResponse(callback, 200, null, xml)
                        break;
                    case HttpConnector.RestCommand.GET_DEVICE_LIST:
                        controllerConnector.handleResponse(callback, 200, null,
                        '''
                        [
                            {
                                "id": 1,
                                "name": "TestDevice"
                            }
                        ]
                        '''.getBytes("UTF-8"))
                        break;
                    case HttpConnector.RestCommand.GET_DEVICE:
                        controllerConnector.handleResponse(callback, 200, null,
                        '''
                        {
                            "commands": [
                                {
                                    "id": "17",
                                    "name": "Light1On",
                                    "protocol": "virtual"
                                },
                                {
                                    "id": "16",
                                    "name": "Light1Status",
                                    "protocol": "virtual"
                                },
                                {
                                    "id": "19",
                                    "name": "TVOn",
                                    "protocol": "virtual"
                                },
                                {
                                    "id": "18",
                                    "name": "Light1BrightnessStatus",
                                    "protocol": "virtual"
                                },
                                {
                                    "id": "13",
                                    "name": "TVOff",
                                    "protocol": "virtual"
                                },
                                {
                                    "id": "14",
                                    "name": "Light1BrightnessSet",
                                    "protocol": "virtual"
                                },
                                {
                                    "id": "15",
                                    "name": "Light1Off",
                                    "protocol": "virtual"
                                }
                            ],
                            "id": "1",
                            "name": "TestDevice",
                            "sensors": [
                                {
                                    "command_id": "16",
                                    "id": "107970902",
                                    "name": "Light1Switch",
                                    "type": "switch"
                                },
                                {
                                    "command_id": "18",
                                    "id": "107970901",
                                    "name": "Light1Slider",
                                    "type": "level"
                                }
                            ]
                        }
                        '''.getBytes("UTF-8"))
                        break;
                    case HttpConnector.RestCommand.GET_SENSOR_STATUS:
                        controllerConnector.handleResponse(callback, 200, null,
                        '''
                        {
                            "status": [
                                {
                                    "content": "0",
                                    "id": 107970901
                                },
                                {
                                    "content": "off",
                                    "id": 107970902
                                }
                            ]
                        }
                        '''.getBytes("UTF-8"))
                        break;
                    case HttpConnector.RestCommand.DO_SENSOR_POLLING:
                        System.out.println("Do sensor polling");
                        break;
                    default:
                        break;
                }
        }

        and: "A spy of the controller that uses the spy connector"
            def controller = Spy(Controller, constructorArgs: [controllerConnector])
            controller.setControllerInfo(new ControllerInfo("http://10.0.0.123:8080/controller"))
            controller.moniitorSensors(_) >> {org.openremote.console.controller.RegistrationHandle handle ->
                System.out.println("################ MONITOR SENSORS SPY ################")
            }

        and: "a spy of the controller adapter that uses the spy controller"
            def controllerAdapter = Spy(Controller2Adapter, constructorArgs: [new URL("http://10.0.0.123:8080"), null, null, controller])

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

        and: "a clean result state"
        def conditions = new PollingConditions(timeout: 10)

        when: "the server container is started"
        def serverPort = findEphemeralPort();
        def container = startContainer(defaultConfig(serverPort), defaultServices(controllerService))

        then: "the service should receive the devices from the inventory"
        conditions.eventually {
            assert controllerService.addedDevices.size() == 1
        }

        and: "the server should be stopped"
        stopContainer(container);
    }

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

    /* TODO broken test
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
