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
import org.openremote.agent.protocol.controller.ControllerAgent
import org.openremote.agent.protocol.controller.ControllerAgentLink
import org.openremote.agent.protocol.controller.ControllerProtocol
import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.model.Constants
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.AttributeRef
import org.openremote.model.attribute.MetaItem
import com.fasterxml.jackson.databind.node.ObjectNode
import org.openremote.model.util.ValueUtil
import org.openremote.test.ManagerContainerTrait
import spock.lang.Ignore
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import javax.ws.rs.client.ClientRequestContext
import javax.ws.rs.client.ClientRequestFilter
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo

import static org.openremote.model.value.ValueType.*
import static org.openremote.model.value.MetaItemType.*

@Ignore // Controller protocol doesn't cancel scheduled tasks and this test intermittently fails
class ControllerProtocolTest extends Specification implements ManagerContainerTrait {
    def CONTROLLER_AGENT_NAME_1 = "Test controller 1"
    def CONTROLLER_AGENT_NAME_2 = "Test controller 2"
    def CONTROLLER_AGENT_NAME_3 = "Test controller 3"

    @SuppressWarnings("GroovyAccessibility")
    def "Check Controller connection"() {
        given: "a mock controller"
        def mockController = new ClientRequestFilter() {
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
                                    ObjectNode body = ValueUtil.parse(bodyStr).orElse(null)

                                    if (body != null && body.get("parameter").isPresent() && body.get("parameter").orElse(null).toString() == "a_parameter") {
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

                requestContext.abortWith(Response.serverError().build())
            }
        }

        and: "expected conditions"
        def conditions = new PollingConditions(timeout: 20, initialDelay: 1)

        and: "the container starts"
        def container = startContainer(defaultConfig(), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def agentService = container.getService(AgentService.class)

        when: "the web target builder is configured to use the mock server"
        if (!controllerProtocol.client.configuration.isRegistered(mockController)) {
            controllerProtocol.client.register(mockController, Integer.MAX_VALUE)
        }

        and: "a Controller agent1 is created"
        def agent1 = new ControllerAgent(CONTROLLER_AGENT_NAME_1)
            .setRealm(Constants.MASTER_REALM)
            .setControllerURI("http://mockapi:8688/controller")

        agent1 = assetStorageService.merge(agent1)

        then: "the protocol instance should authenticate and start pinging the server and the connection status should become CONNECTED"
        conditions.eventually {
            assert agentService.getAgent(agent1.id) != null
            assert agentService.getAgent(agent1.id).getAgentStatus().orElse(null) == ConnectionStatus.CONNECTED
            assert (ControllerProtocol)agentService.getProtocolInstance(agent1.id) != null
            assert ((ControllerProtocol)agentService.getProtocolInstance(agent1.id)).controllerHeartbeat.isCancelled()
        }

        when: "an asset is created with attributes linked to the controller agent"
        def asset = new ThingAsset("Test Asset")
            .setParent(agent1)
            .addOrReplaceAttributes(
                // attribute that sends requests to the server using PUT with dynamic body and custom header to override parent
                new Attribute<>("sensor", TEXT)
                    .addMeta(
                        new MetaItem<>(AGENT_LINK, new ControllerAgentLink(agent1.id, "MyDevice")
                            .setSensorName("my_sensor1a")),
                        new MetaItem<>(READ_ONLY)
                    ),
                new Attribute<>("command", TEXT)
                    .addMeta(
                        new MetaItem<>(AGENT_LINK, new ControllerAgentLink(agent1.id, "MyDevice")
                            .setCommandName("my_command"))
                    )
        )

        and: "the asset is merged into the asset service"
        asset = assetStorageService.merge(asset)

        then: "multiple pollings should have occurred"
        conditions.eventually {
            assert mockController.pollCount > 2
        }

        then:
        conditions.eventually {
            def newAsset = assetStorageService.find(asset.getId(), true)
            assert controllerProtocol.controllersMap.get(controllerRef).getSensorsListForDevice("MyDevice").size() == 1
            assert controllerProtocol.pollingSensorList.size() == 1
            assert mockController.commandCount == 0
            assert mockController.pollCount > 0
            assert newAsset.getAttribute("sensor").flatMap({it.getValue()}).orElse("") == "newValue1a"
        }

        when: "another agent is created that is linked to an unavailable controller"
        def agent2 = new ControllerAgent(CONTROLLER_AGENT_NAME_1)
            .setRealm(Constants.MASTER_REALM)
            .setControllerURI("http://mockapi:8688/controller")

        agent2 = assetStorageService.merge(agent2)

        then: "the new agent should be DISCONNECTED but the old agent should still show as CONNECTED"
        conditions.eventually {
            assert agentService.getAgent(agent1.id) != null
            assert agentService.getAgent(agent2.id) != null
            assert agentService.getAgent(agent1.id).getAgentStatus().orElse(null) == ConnectionStatus.CONNECTED
            assert agentService.getAgent(agent2.id).getAgentStatus().orElse(null) == ConnectionStatus.DISCONNECTED
            assert (ControllerProtocol)agentService.getProtocolInstance(agent1.id) != null
            assert (ControllerProtocol)agentService.getProtocolInstance(agent2.id) != null
            assert ((ControllerProtocol)agentService.getProtocolInstance(agent1.id)).controllerHeartbeat.isCancelled()
            assert !((ControllerProtocol)agentService.getProtocolInstance(agent2.id)).controllerHeartbeat.isCancelled()
        }

        when: "this new agent is removed and another one added"
        assetStorageService.delete([agent2.id])
        def agent3 = new ControllerAgent(CONTROLLER_AGENT_NAME_3)
            .setRealm(Constants.MASTER_REALM)
            .setControllerURI("http://mockapi:8688/controller")
        agent3 = assetStorageService.merge(agent3)

        then: "the protocol should authenticate and start pinging the server and the connection status should become CONNECTED"
        conditions.eventually {
            assert agentService.getAgent(agent1.id) != null
            assert agentService.getAgent(agent2.id) == null
            assert agentService.getAgent(agent3.id) != null
            assert agentService.getAgent(agent1.id).getAgentStatus().orElse(null) == ConnectionStatus.CONNECTED
            assert agentService.getAgent(agent3.id).getAgentStatus().orElse(null) == ConnectionStatus.CONNECTED
            assert (ControllerProtocol)agentService.getProtocolInstance(agent1.id) != null
            assert (ControllerProtocol)agentService.getProtocolInstance(agent2.id) == null
            assert (ControllerProtocol)agentService.getProtocolInstance(agent3.id) != null
            assert ((ControllerProtocol)agentService.getProtocolInstance(agent1.id)).controllerHeartbeat.isCancelled()
            assert ((ControllerProtocol)agentService.getProtocolInstance(agent3.id)).controllerHeartbeat.isCancelled()
        }

        when: "an asset is created with attributes linked to the controller agent"
        def asset2 = new ThingAsset("Test Asset2")
            .setParent(agent3)
            .addOrReplaceAttributes(
                // attribute that sends requests to the server using PUT with dynamic body and custom header to override parent
                new Attribute<>("command", TEXT, "command1")
                    .addMeta(
                        new MetaItem<>(AGENT_LINK, new ControllerAgentLink(agent3.id, "DeviceName2")
                            .setCommandsMap(new HashMap<String, List<String>>([
                                ("command1") : ["my_command"],
                                ("command2") : ["wrong"]
                            ]))
                        )
                    )
            )
        asset2 = assetStorageService.merge(asset2)

        then: "the asset should be linked to the protocol"
        conditions.eventually {
            assert controllerProtocol.linkedAttributes.containsKey(new AttributeRef(asset2.id, CONTROLLER_AGENT_NAME_3))
        }

        when: "a linked attribute value is updated"
        def attributeEvent = new AttributeEvent(asset2.id,
                "command",
                "command1")
        assetProcessingService.sendAttributeEvent(attributeEvent)

        then:
        conditions.eventually {
            assert mockController.commandCount == 1
        }

        and: "another linked attribute value is updated with no value"
        def attributeEvent2 = new AttributeEvent(asset.id,
                "command",
                null)
        assetProcessingService.sendAttributeEvent(attributeEvent2)

        then:
        conditions.eventually {
            assert mockController.commandCount == 1
        }

        and: "another linked attribute value is updated with a value"
        def attributeEvent3 = new AttributeEvent(asset.id,
                "command",
                "a_parameter")
        assetProcessingService.sendAttributeEvent(attributeEvent3)

        then:
        conditions.eventually {
            assert mockController.commandCount == 2
            assert true
        }
    }
}
