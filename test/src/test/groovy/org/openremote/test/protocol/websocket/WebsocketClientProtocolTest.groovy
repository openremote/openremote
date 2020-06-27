/*
 * Copyright 2019, OpenRemote Inc.
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
package org.openremote.test.protocol.websocket

import org.openremote.agent.protocol.Protocol
import org.openremote.container.web.OAuthPasswordGrant
import org.openremote.agent.protocol.simulator.SimulatorProtocol
import org.openremote.agent.protocol.websocket.WebsocketClientProtocol
import org.openremote.agent.protocol.websocket.WebsocketHttpSubscription
import org.openremote.agent.protocol.websocket.WebsocketSubscription
import org.openremote.container.Container
import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.setup.SetupService
import org.openremote.manager.setup.builtin.ManagerDemoSetup
import org.openremote.model.Constants
import org.openremote.model.asset.*
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.attribute.*
import org.openremote.model.event.TriggeredEventSubscription
import org.openremote.model.event.shared.EventSubscription
import org.openremote.model.event.shared.SharedEvent
import org.openremote.model.query.AssetQuery
import org.openremote.model.query.filter.StringPredicate
import org.openremote.model.value.*
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import javax.ws.rs.HttpMethod
import javax.ws.rs.client.ClientRequestContext
import javax.ws.rs.client.ClientRequestFilter
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.security.ManagerIdentityProvider.SETUP_ADMIN_PASSWORD
import static org.openremote.manager.security.ManagerIdentityProvider.SETUP_ADMIN_PASSWORD_DEFAULT
import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID
import static org.openremote.model.Constants.MASTER_REALM_ADMIN_USER
import static org.openremote.model.asset.agent.ProtocolConfiguration.initProtocolConfiguration

class WebsocketClientProtocolTest extends Specification implements ManagerContainerTrait {

    @Shared
    def mockServer = new ClientRequestFilter() {

        private boolean subscriptionDone = false

        @Override
        void filter(ClientRequestContext requestContext) throws IOException {
            def requestUri = requestContext.uri
            def requestPath = requestUri.scheme + "://" + requestUri.host + requestUri.path

            // Check auth header is present
            def authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)
            if (authHeader == null || authHeader.length() < 8) {
                requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build())
                return
            }

            switch (requestPath) {

                case "https://mockapi/assets":
                    if (requestContext.method == HttpMethod.POST
                        && requestContext.getHeaderString("header1") == "header1Value1"
                        && requestContext.getHeaderString("header2") == "header2Value1,header2Value2"
                        && requestContext.getHeaderString("Content-type") == MediaType.APPLICATION_JSON) {

                        String bodyStr = (String)requestContext.getEntity()
                        AssetQuery assetQuery = Container.JSON.readValue(bodyStr, AssetQuery.class)
                        if (assetQuery != null && assetQuery.ids != null && assetQuery.ids.size() == 1) {
                            subscriptionDone = true
                            requestContext.abortWith(Response.ok().build())
                            return
                        }
                    }
                    break
            }

            requestContext.abortWith(Response.serverError().build())
        }
    }

    @SuppressWarnings("GroovyAccessibility")
    def "Check websocket client protocol configuration and linked attribute deployment"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 1)

        when: "the container starts"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def websocketClientProtocol = container.getService(WebsocketClientProtocol.class)
        def simulatorProtocol = container.getService(SimulatorProtocol.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def agentService = container.getService(AgentService.class)
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)

        then: "the container should be running"
        conditions.eventually {
            assert container.isRunning()
        }

        when: "the web target builder is configured to use the mock HTTP server (to test subscriptions)"
        if (!websocketClientProtocol.client.configuration.isRegistered(mockServer)) {
            websocketClientProtocol.client.register(mockServer, Integer.MAX_VALUE)
        }

        and: "an agent with a websocket client protocol configuration is created to connect to this tests manager"
        def agent = new Asset()
        agent.setRealm(Constants.MASTER_REALM)
        agent.setName("Test Agent")
        agent.setType(AssetType.AGENT)
        agent.setAttributes(
            initProtocolConfiguration(new AssetAttribute("protocolConfig"), WebsocketClientProtocol.PROTOCOL_NAME)
                .addMeta(
                    new MetaItem(
                        WebsocketClientProtocol.META_PROTOCOL_CONNECT_URI,
                        Values.create("ws://localhost:$serverPort/websocket/events?Auth-Realm=master")
                    ),
                    new MetaItem(
                        Protocol.META_PROTOCOL_OAUTH_GRANT,
                        new OAuthPasswordGrant("http://localhost:$serverPort/auth/realms/master/protocol/openid-connect/token",
                            KEYCLOAK_CLIENT_ID,
                            null,
                            null,
                            MASTER_REALM_ADMIN_USER,
                            getString(container.getConfig(), SETUP_ADMIN_PASSWORD, SETUP_ADMIN_PASSWORD_DEFAULT)).toObjectValue()
                    ),
                    new MetaItem(
                        WebsocketClientProtocol.META_SUBSCRIPTIONS,
                        Values.parse(
                            Container.JSON.writeValueAsString(Arrays.asList(
                                new WebsocketSubscription().body(EventSubscription.SUBSCRIBE_MESSAGE_PREFIX + Container.JSON.writeValueAsString(
                                    new EventSubscription(
                                        AttributeEvent.class,
                                        new AssetFilter<AttributeEvent>().setAssetIds(managerDemoSetup.apartment1LivingroomId),
                                        "1",
                                        null))),
                                new WebsocketHttpSubscription()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .method(WebsocketHttpSubscription.Method.POST)
                                    .headers(Values.<ObjectValue>parse(/{"header1": "header1Value1", "header2": ["header2Value1","header2Value2"]}/).get())
                                    .uri("https://mockapi/assets")
                                    .body(
                                        Container.JSON.writeValueAsString(new AssetQuery().ids(managerDemoSetup.apartment1LivingroomId))
                                    )
                            ))
                        ).orElse(null)
                    )
                )
        )

        and: "the agent is added to the asset service"
        agent = assetStorageService.merge(agent)

        then: "the protocol should authenticate and the connection status should become CONNECTED"
        conditions.eventually {
            def status = agentService.getProtocolConnectionStatus(new AttributeRef(agent.id, "protocolConfig"))
            assert status == ConnectionStatus.CONNECTED
        }

        and: "the subscriptions should have been executed"
        conditions.eventually {
            assert mockServer.subscriptionDone
        }

        when: "an asset is created with attributes linked to the protocol configuration"
        def asset = new Asset("Test Asset", AssetType.THING, agent)
        asset.setAttributes(
            // write attribute value
            new AssetAttribute("readWriteTargetTemp", AttributeValueType.NUMBER)
                .addMeta(
                    new MetaItem(MetaItemType.AGENT_LINK, new AttributeRef(agent.id, "protocolConfig").toArrayValue()),
                    new MetaItem(Protocol.META_ATTRIBUTE_WRITE_VALUE, Values.create("\'" + SharedEvent.MESSAGE_PREFIX +
                        Container.JSON.writeValueAsString(new AttributeEvent(
                            managerDemoSetup.apartment1LivingroomId,
                            "targetTemperature",
                            Values.create(0.12345))).replace("0.12345", Protocol.DYNAMIC_VALUE_PLACEHOLDER) + "\'")),
                    new MetaItem(WebsocketClientProtocol.META_ATTRIBUTE_MATCH_FILTERS,
                        Values.convertToValue(
                            [
                                new SubStringValueFilter(TriggeredEventSubscription.MESSAGE_PREFIX.length()),
                                new JsonPathFilter("\$..attributeState.attributeRef.attributeName", false, false)
                            ] as ValueFilter[]
                            , Container.JSON.writer()).orElse(null)),
                    new MetaItem(WebsocketClientProtocol.META_ATTRIBUTE_MATCH_PREDICATE,
                        new StringPredicate(AssetQuery.Match.CONTAINS, true, "targetTemperature").toModelValue()),
                    new MetaItem(Protocol.META_ATTRIBUTE_VALUE_FILTERS,
                        Values.convertToValue(
                            [
                                new SubStringValueFilter(TriggeredEventSubscription.MESSAGE_PREFIX.length()),
                                new JsonPathFilter("\$..events[?(@.attributeState.attributeRef.attributeName == \"targetTemperature\")].attributeState.value", true, false)
                            ] as ValueFilter[]
                            , Container.JSON.writer()).orElse(null)),
                    new MetaItem(WebsocketClientProtocol.META_SUBSCRIPTIONS,
                        Values.convertToValue(
                            [
                                new WebsocketSubscription().body(SharedEvent.MESSAGE_PREFIX + Container.JSON.writeValueAsString(
                                    new ReadAssetAttributeEvent(managerDemoSetup.apartment1LivingroomId, "targetTemperature")
                                ))
                            ], Container.JSON.writer()).orElse(null))
                ),
            new AssetAttribute("readCo2Level", AttributeValueType.NUMBER)
                .addMeta(
                    new MetaItem(MetaItemType.AGENT_LINK, new AttributeRef(agent.id, "protocolConfig").toArrayValue()),
                        new MetaItem(MetaItemType.READ_ONLY),
                    new MetaItem(WebsocketClientProtocol.META_ATTRIBUTE_MATCH_FILTERS,
                        Values.convertToValue(
                            [
                                new SubStringValueFilter(TriggeredEventSubscription.MESSAGE_PREFIX.length()),
                                new JsonPathFilter("\$..attributeState.attributeRef.attributeName", false, false)
                            ] as ValueFilter[]
                            , Container.JSON.writer()).orElse(null)),
                    new MetaItem(WebsocketClientProtocol.META_ATTRIBUTE_MATCH_PREDICATE,
                        new StringPredicate(AssetQuery.Match.CONTAINS, "co2Level").toModelValue()),
                    new MetaItem(Protocol.META_ATTRIBUTE_VALUE_FILTERS,
                        Values.convertToValue(
                            [
                                new SubStringValueFilter(TriggeredEventSubscription.MESSAGE_PREFIX.length()),
                                new JsonPathFilter("\$..events[?(@.attributeState.attributeRef.attributeName == \"co2Level\")].attributeState.value", true, false),
                            ] as ValueFilter[]
                            , Container.JSON.writer()).orElse(null)),
                    new MetaItem(WebsocketClientProtocol.META_SUBSCRIPTIONS,
                        Values.convertToValue(
                            [
                                new WebsocketSubscription().body(SharedEvent.MESSAGE_PREFIX + Container.JSON.writeValueAsString(
                                    new ReadAssetAttributeEvent(managerDemoSetup.apartment1LivingroomId, "co2Level")
                                ))
                            ]
                            , Container.JSON.writer()).orElse(null))
                )
        )

        and: "the asset is merged into the asset service"
        asset = assetStorageService.merge(asset)

        then: "the linked attributes should have no initial values"
        conditions.eventually {
            asset = assetStorageService.find(asset.getId(), true)
            assert !asset.getAttribute("readCo2Level").get().value.isPresent()
            assert !asset.getAttribute("readWriteTargetTemp").get().value.isPresent()
        }

        when: "a linked attribute value is updated"
        def attributeEvent = new AttributeEvent(asset.id,
            "readWriteTargetTemp",
            Values.create(19.5))
        assetProcessingService.sendAttributeEvent(attributeEvent)

        then: "the linked targetTemperature attribute should contain this written value (it should have been written to the target temp attribute and then read back again)"
        conditions.eventually {
            asset = assetStorageService.find(asset.getId(), true)
            assert asset.getAttribute("readWriteTargetTemp").flatMap{it.getValueAsNumber()}.orElse(null) == 19.5d
        }

        when: "the co2level changes"
        def co2LevelIncrement = new AttributeEvent(
            managerDemoSetup.apartment1LivingroomId, "co2Level", Values.create(600)
        )
        simulatorProtocol.putValue(co2LevelIncrement)

        then: "the linked co2Level attribute should get the new value"
        conditions.eventually {
            asset = assetStorageService.find(asset.getId(), true)
            assert asset.getAttribute("readCo2Level").flatMap{it.getValueAsNumber()}.orElse(null) == 600d
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
