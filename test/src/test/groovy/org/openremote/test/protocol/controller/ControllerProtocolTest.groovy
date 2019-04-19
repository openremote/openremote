/*
 * Copyright 2017, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.test.protocol.controller

import org.jboss.resteasy.spi.ResteasyUriInfo
import org.jboss.resteasy.util.BasicAuthHelper
import org.openremote.agent.protocol.controller.ControllerProtocol
import org.openremote.agent.protocol.http.WebTargetBuilder
import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.model.Constants
import org.openremote.model.asset.Asset
import org.openremote.model.asset.AssetAttribute
import org.openremote.model.attribute.MetaItemType
import org.openremote.model.asset.AssetType
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.asset.agent.ProtocolConfiguration
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.AttributeRef
import org.openremote.model.attribute.AttributeValueType
import org.openremote.model.attribute.MetaItem
import org.openremote.model.value.ObjectValue
import org.openremote.model.value.Values
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import javax.ws.rs.client.ClientRequestContext
import javax.ws.rs.client.ClientRequestFilter
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo

class ControllerProtocolTest extends Specification implements ManagerContainerTrait {
    def CONTROLLERPROTOCOL_ATTRIBUTE_NAME = "testcontrollerConfig"
    def CONTROLLERPROTOCOL_ATTRIBUTE_NAME2 = "testcontrollerConfig2"
    def CONTROLLERPROTOCOL_ATTRIBUTE_NAME3 = "testcontrollerConfig3"

    @Shared
    def mockServer = new ClientRequestFilter() {
        private int commandCount = 0
        private int pollCount = 0

        @Override
        void filter(ClientRequestContext requestContext) throws IOException, ConnectException {
            def requestUri = requestContext.uri
            def requestPath = requestUri.scheme + "://" + requestUri.host + (requestUri.port.toString().size() != 0 ? (":" + requestUri.port) : "") + requestUri.path

            def pollingReq = requestUri.path ==~ /\/controller\/rest\/devices\/MyDevice\/polling\/OR3ControllerProtocol_[0-9a-zA-Z]*_[0-9a-zA-Z]*/

            if(pollingReq) {
                UriInfo uriInfo = new ResteasyUriInfo(requestContext.uri)
                def queryParams = uriInfo.getQueryParameters(true)

                if (queryParams.get("name").size() == 2 && queryParams.getFirst("name") == "my_sensor1" && queryParams.get("name").get(1) == "my_sensor2"){
                    requestContext.abortWith(
                            Response.ok("[{\"name\": \"my_sensor1\", \"value\": \"newValue1\"},{\"name\": \"my_sensor2\", \"value\": \"newValue2\"}]", MediaType.APPLICATION_JSON).build()
                    )
                    pollCount++
                    return
                } else if (queryParams.get("name").size() == 2 && queryParams.getFirst("name") == "my_sensor1b" && queryParams.get("name").get(1) == "my_sensor2"){
                    requestContext.abortWith(
                            Response.ok("[{\"name\": \"my_sensor2\", \"value\": \"newValue2\"}]", MediaType.APPLICATION_JSON).build()
                    )
                    pollCount++
                    return
                } else if (queryParams.get("name").size() == 1 && queryParams.getFirst("name") == "my_sensor1a"){
                    requestContext.abortWith(
                            Response.ok("[{\"name\": \"my_sensor1a\", \"value\": \"newValue1a\"}]", MediaType.APPLICATION_JSON).build()
                    )
                    pollCount++
                    return
                } else {
                    requestContext.abortWith(Response.serverError().build())
                    return
                }
            } else {
                switch (requestPath) {
                    case "http://mockapi:8688/controller":
                        requestContext.abortWith(Response.ok().build())
                        return
                        break
                    case "http://basicauthmockapi:8688/controller":
                        def authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)
                        if (authHeader != null) {
                            def usernameAndPassword = BasicAuthHelper.parseHeader(authHeader)
                            if (usernameAndPassword != null
                                    && usernameAndPassword[0] == "testuser"
                                    && usernameAndPassword[1] == "password1") {
                                requestContext.abortWith(Response.ok().build())
                                return
                            }
                        } else {
                            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build())
                            return
                        }
                        break
                    case "http://disconnectedmockapi:8688/controller":
                        throw new ConnectException("Server not available")
                        return
                        break
                }

                if(requestContext.method == "POST") {
                    switch (requestPath) {
                        case "http://mockapi:8688/controller/rest/devices/DeviceName2/commands":
                            UriInfo uriInfo = new ResteasyUriInfo(requestContext.uri)
                            def queryParams = uriInfo.getQueryParameters(true)
                            if (queryParams.get("name").size() == 1 && queryParams.getFirst("name") == "my_command") {
                                requestContext.abortWith(
                                        Response.ok().build()
                                )
                                commandCount++
                                return
                            } else {
                                requestContext.abortWith(Response.serverError().build())
                                return
                            }
                            break
                        case "http://mockapi:8688/controller/rest/devices/MyDevice/commands":
                            UriInfo uriInfo = new ResteasyUriInfo(requestContext.uri)
                            def queryParams = uriInfo.getQueryParameters(true)
                            if (queryParams.get("name").size() == 1 && queryParams.getFirst("name") == "my_command") {
                                String bodyStr = (String)requestContext.getEntity()
                                ObjectValue body = Values.<ObjectValue>parse(bodyStr).orElse(null)

                                if (body.get("parameter").isPresent() && body.get("parameter").orElse(null).toString() == "a_parameter") {
                                    requestContext.abortWith(Response.ok().build())
                                    commandCount++
                                    return
                                } else {
                                    requestContext.abortWith(Response.serverError().build())
                                    return
                                }
                            } else {
                                requestContext.abortWith(Response.serverError().build())
                                return
                            }
                            break
                    }
                }
            }
        }
    }

    def cleanupSpec() {
        WebTargetBuilder.close()
    }

    def cleanup() {
        mockServer.commandCount = 0
        mockServer.pollCount = 0
    }

    def "Check Controller connection"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, initialDelay: 1)

        when: "the container starts"
        def serverPort = findEphemeralPort()
        def container = startContainerNoDemoImport(defaultConfig(serverPort), defaultServices())
        def controllerProtocol = container.getService(ControllerProtocol.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def agentService = container.getService(AgentService.class)

        then: "the container should be running"
        conditions.eventually {
            assert container.isRunning()
        }

        when: "the web target builder is configured to use the mock server"
        // Need to do this here as HTTP protocol must be initialised first
        WebTargetBuilder.initClient()
        if (!WebTargetBuilder.client.configuration.isRegistered(mockServer)) {
            WebTargetBuilder.client.register(mockServer, Integer.MAX_VALUE)
        }

        and: "an agent with a Controller protocol configuration is created"
        def agent = new Asset()
        agent.setRealm(Constants.MASTER_REALM)
        agent.setName("Test Agent")
        agent.setType(AssetType.AGENT)
        agent.setAttributes(
                ProtocolConfiguration.initProtocolConfiguration(new AssetAttribute(CONTROLLERPROTOCOL_ATTRIBUTE_NAME), ControllerProtocol.PROTOCOL_NAME)
                        .addMeta(
                        new MetaItem(
                                ControllerProtocol.META_PROTOCOL_BASE_URI,
                                Values.create("http://mockapi:8688/controller")
                        )
                )
        )

        and: "the agent is added to the asset service"
        agent = assetStorageService.merge(agent)
        def controllerRef = assetStorageService.find(agent.id, true).getAttribute(CONTROLLERPROTOCOL_ATTRIBUTE_NAME).get().getReferenceOrThrow()

        then: "the protocol should authenticate and start pinging the server and the connection status should become CONNECTED"
        conditions.eventually {
            def status = agentService.getProtocolConnectionStatus(new AttributeRef(agent.id, CONTROLLERPROTOCOL_ATTRIBUTE_NAME))

            assert status == ConnectionStatus.CONNECTED
            assert controllerProtocol.controllerHeartbeat.get(controllerRef).isCancelled()
        }

        when: "an asset is created with attributes linked to the controller protocol configuration"
        def asset = new Asset("Test Asset", AssetType.THING, agent)
        asset.setAttributes(
                // attribute that sends requests to the server using PUT with dynamic body and custom header to override parent
                new AssetAttribute("sensor", AttributeValueType.STRING)
                        .addMeta(
                        new MetaItem(MetaItemType.AGENT_LINK, new AttributeRef(agent.id, CONTROLLERPROTOCOL_ATTRIBUTE_NAME).toArrayValue()),
                        new MetaItem(ControllerProtocol.META_ATTRIBUTE_DEVICE_NAME, Values.create("MyDevice")),
                        new MetaItem(ControllerProtocol.META_ATTRIBUTE_SENSOR_NAME, Values.create("my_sensor1a")),
                        new MetaItem(MetaItemType.READ_ONLY, Values.create(true))
                ),
                new AssetAttribute("command", AttributeValueType.STRING)
                        .addMeta(
                        new MetaItem(MetaItemType.AGENT_LINK, new AttributeRef(agent.id, CONTROLLERPROTOCOL_ATTRIBUTE_NAME).toArrayValue()),
                        new MetaItem(ControllerProtocol.META_ATTRIBUTE_DEVICE_NAME, Values.create("MyDevice")),
                        new MetaItem(ControllerProtocol.META_ATTRIBUTE_COMMAND_NAME, Values.create("my_command"))
                )
        )

        and: "the asset is merged into the asset service"
        asset = assetStorageService.merge(asset)

        then: "multiple pollings should have occurred"
        conditions.eventually {
            assert mockServer.pollCount > 2
        }

        and: "value should be updated"
        def newAsset = assetStorageService.find(asset.getId(), true)

        then:
        conditions.eventually {
            assert controllerProtocol.controllersMap.get(controllerRef).getSensorsListForDevice("MyDevice").size() == 1
            assert controllerProtocol.pollingSensorList.size() == 1
            assert mockServer.commandCount == 0
            assert mockServer.pollCount > 0
            assert newAsset.getAttribute("sensor").flatMap({it.getValueAsString()}).orElse("") == "newValue1a"
        }

        when: "another protocol is added linked to an unavailable controller"
        agent.addAttributes(ProtocolConfiguration.initProtocolConfiguration(new AssetAttribute(CONTROLLERPROTOCOL_ATTRIBUTE_NAME2), ControllerProtocol.PROTOCOL_NAME)
                .addMeta(
                new MetaItem(
                        ControllerProtocol.META_PROTOCOL_BASE_URI,
                        Values.create("http://disconnectedmockapi:8688/controller")
                )
        ))

        and: "the agent is added to the asset service"
        agent = assetStorageService.merge(agent)
        def controllerRef2 = assetStorageService.find(agent.id, true).getAttribute(CONTROLLERPROTOCOL_ATTRIBUTE_NAME2).get().getReferenceOrThrow()

        then: "the protocol should authenticate and start pinging the server and the connection status should become CONNECTED"
        conditions.eventually {
            def status2 = agentService.getProtocolConnectionStatus(new AttributeRef(agent.id, CONTROLLERPROTOCOL_ATTRIBUTE_NAME2))
            def status = agentService.getProtocolConnectionStatus(new AttributeRef(agent.id, CONTROLLERPROTOCOL_ATTRIBUTE_NAME))

            assert status2 == ConnectionStatus.DISCONNECTED
            assert status == ConnectionStatus.CONNECTED
            assert !controllerProtocol.controllerHeartbeat.get(controllerRef2).isCancelled()
            assert controllerProtocol.controllerHeartbeat.get(controllerRef).isCancelled()
        }

        when: "another protocol for command with map"
        agent.addAttributes(ProtocolConfiguration.initProtocolConfiguration(new AssetAttribute(CONTROLLERPROTOCOL_ATTRIBUTE_NAME3), ControllerProtocol.PROTOCOL_NAME)
                .addMeta(
                new MetaItem(
                        ControllerProtocol.META_PROTOCOL_BASE_URI,
                        Values.create("http://mockapi:8688/controller")
                )
        ))

        and: "the agent is added to the asset service"
        agent = assetStorageService.merge(agent)
        def controllerRef3 = assetStorageService.find(agent.id, true).getAttribute(CONTROLLERPROTOCOL_ATTRIBUTE_NAME3).get().getReferenceOrThrow()

        then: "the protocol should authenticate and start pinging the server and the connection status should become CONNECTED"
        conditions.eventually {
            def status = agentService.getProtocolConnectionStatus(new AttributeRef(agent.id, CONTROLLERPROTOCOL_ATTRIBUTE_NAME3))

            assert status == ConnectionStatus.CONNECTED
            assert controllerProtocol.controllerHeartbeat.get(controllerRef3).isCancelled()
        }

        when: "an asset is created with attributes linked to the controller protocol configuration"
        def asset2 = new Asset("Test Asset2", AssetType.THING, agent)
        asset2.setAttributes(
                // attribute that sends requests to the server using PUT with dynamic body and custom header to override parent
                new AssetAttribute("command", AttributeValueType.STRING, Values.create("command1"))
                        .addMeta(
                        new MetaItem(MetaItemType.AGENT_LINK, new AttributeRef(agent.id, CONTROLLERPROTOCOL_ATTRIBUTE_NAME3).toArrayValue()),
                        new MetaItem(ControllerProtocol.META_ATTRIBUTE_DEVICE_NAME, Values.create("DeviceName2")),
                        new MetaItem(ControllerProtocol.META_ATTRIBUTE_COMMANDS_MAP, Values.<ObjectValue>parse(/{"command1": "my_command", "command2": "wrong"}/).get())
                        )
        )

        and: "the asset is merged into the asset service"
        asset2 = assetStorageService.merge(asset2)

        and: "a linked attribute value is updated"
        def attributeEvent = new AttributeEvent(asset2.id,
                "command",
                Values.create("command1"))
        assetProcessingService.sendAttributeEvent(attributeEvent)

        then:
        conditions.eventually {
            assert controllerProtocol.controllersTargetMap.size() == 3
            assert mockServer.commandCount == 1
        }

        and: "another linked attribute value is updated with no value"
        def attributeEvent2 = new AttributeEvent(asset.id,
                "command",
                null)
        assetProcessingService.sendAttributeEvent(attributeEvent2)

        then:
        conditions.eventually {
            assert mockServer.commandCount == 1
        }

        and: "another linked attribute value is updated with a value"
        def attributeEvent3 = new AttributeEvent(asset.id,
                "command",
                Values.create("a_parameter"))
        assetProcessingService.sendAttributeEvent(attributeEvent3)

        then:
        conditions.eventually {
            assert mockServer.commandCount == 2
            assert true
        }
    }
}