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
package org.openremote.test.protocol.udp

import org.jboss.resteasy.spi.ResteasyUriInfo
import org.jboss.resteasy.util.BasicAuthHelper
import org.openremote.agent.protocol.Protocol
import org.openremote.agent.protocol.ProtocolExecutorService
import org.openremote.agent.protocol.filter.RegexFilter
import org.openremote.agent.protocol.http.*
import org.openremote.agent.protocol.udp.UdpClientProtocol
import org.openremote.agent.protocol.udp.UdpStringServer
import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.model.Constants
import org.openremote.model.asset.Asset
import org.openremote.model.asset.AssetAttribute
import org.openremote.model.asset.AssetType
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.attribute.*
import org.openremote.model.value.ObjectValue
import org.openremote.model.value.Value
import org.openremote.model.value.Values
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import javax.ws.rs.HttpMethod
import javax.ws.rs.client.ClientRequestContext
import javax.ws.rs.client.ClientRequestFilter
import javax.ws.rs.core.*

import static org.openremote.model.asset.agent.ProtocolConfiguration.initProtocolConfiguration

class UdpClientProtocolTest extends Specification implements ManagerContainerTrait {

    @Shared
    def mockServer = new ClientRequestFilter() {

        private boolean supportsRefresh
        private int accessTokenCount
        private int refreshTokenCount
        private String accessToken = null
        private String refreshToken = null
        private int pingCount = 0
        private int pollCountFast = 0
        private int pollCountSlow = 0
        private boolean putRequestWithHeadersCalled = false
        private int successFailureCount = 0

        @Override
        void filter(ClientRequestContext requestContext) throws IOException {
            def requestUri = requestContext.uri
            def requestPath = requestUri.scheme + "://" + requestUri.host + requestUri.path

            switch (requestPath) {
                case "https://mockapi/basicauth":
                    def authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)
                    if (authHeader != null) {
                        def usernameAndPassword = BasicAuthHelper.parseHeader(authHeader)
                        if (usernameAndPassword != null
                            && usernameAndPassword[0] == "testuser"
                            && usernameAndPassword[1] == "password1") {
                            requestContext.abortWith(Response.ok().build())
                            return
                        }
                    }
                    break
                case "https://mockapi/token":
                    // OAuth token request extract the grant info
                    def grant = ((Form) requestContext.getEntity()).asMap()
                    if (grant.getFirst(OAuthGrant.VALUE_KEY_GRANT_TYPE) == "password"
                        && grant.getFirst(OAuthGrant.VALUE_KEY_CLIENT_ID) == "TestClient"
                        && grant.getFirst(OAuthGrant.VALUE_KEY_CLIENT_SECRET) == "TestSecret"
                        && grant.getFirst(OAuthGrant.VALUE_KEY_SCOPE) == "scope1 scope2"
                        && grant.getFirst(OAuthPasswordGrant.VALUE_KEY_USERNAME) == "testuser"
                        && grant.getFirst(OAuthPasswordGrant.VALUE_KEY_PASSWORD) == "password") {
                        accessToken = "accesstoken" + accessTokenCount++
                        def response = new OAuthServerResponse()
                        response.accessToken = accessToken
                        response.expiresIn = 100
                        response.tokenType = "Bearer"

                        // Include refresh token if configured to support it
                        if (supportsRefresh) {
                            refreshToken = "refreshtoken" + accessTokenCount
                            response.refreshToken = refreshToken
                        }

                        requestContext.abortWith(
                            Response.ok(response, MediaType.APPLICATION_JSON_TYPE).build()
                        )
                        return
                    } else if (grant.getFirst(OAuthGrant.VALUE_KEY_GRANT_TYPE) == "refresh_token"
                        && grant.getFirst(OAuthGrant.VALUE_KEY_CLIENT_ID) == "TestClient"
                        && grant.getFirst(OAuthGrant.VALUE_KEY_CLIENT_SECRET) == "TestSecret"
                        && grant.getFirst(OAuthGrant.VALUE_KEY_SCOPE) == "scope1 scope2"
                        && grant.getFirst(OAuthRefreshTokenGrant.VALUE_KEY_REFRESH_TOKEN) == refreshToken) {
                        refreshTokenCount++
                        accessToken = "accesstoken" + accessTokenCount++
                        refreshToken = "refreshtoken" + accessTokenCount
                        def response = new OAuthServerResponse()
                        response.accessToken = accessToken
                        response.refreshToken = refreshToken
                        response.expiresIn = 100
                        response.tokenType = "Bearer"

                        requestContext.abortWith(
                            Response.ok(response, MediaType.APPLICATION_JSON_TYPE).build()
                        )
                        return
                    }
                    break
                default:
                    // Check access token is valid
                    def authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)
                    def accessToken = authHeader == null ? null : authHeader.substring(7)
                    if (accessToken == null || this.accessToken == null || accessToken != this.accessToken) {
                        requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build())
                        return
                    }
                    break
            }

            switch (requestPath) {
                case "https://mockapi/pingGet":
                    UriInfo uriInfo = new ResteasyUriInfo(requestContext.uri)
                    def queryParams = uriInfo.getQueryParameters(true)
                    if (queryParams.get("param1").size() == 1
                        && queryParams.getFirst("param1") == "param1Value1"
                        && queryParams.get("param2").size() == 2
                        && queryParams.get("param2").get(0) == "param2Value1"
                        && queryParams.get("param2").get(1) == "param2Value2"
                        && requestContext.getHeaderString("header1") == "header1Value1"
                        && requestContext.getHeaderString("header2") == "header2Value1,header2Value2") {
                        pingCount++
                        requestContext.abortWith(
                            Response.ok().build()
                        )
                        return
                    }
                    break
                case "https://mockapi/pingPost":
                    UriInfo uriInfo = new ResteasyUriInfo(requestContext.uri)
                    def queryParams = uriInfo.getQueryParameters(true)
                    if (queryParams.get("param1").size() == 2
                        && queryParams.getFirst("param1") == "param1Value1"
                        && queryParams.get("param1").get(1) == "param1Value2"
                        && queryParams.get("param2").size() == 2
                        && queryParams.get("param2").get(0) == "param2Value1"
                        && queryParams.get("param2").get(1) == "param2Value2"
                        && queryParams.get("param3").size() == 1
                        && queryParams.getFirst("param3") == "param3Value1"
                        && requestContext.method == HttpMethod.POST
                        && requestContext.getHeaderString("header1") == "header1Value1"
                        && requestContext.getHeaderString("header2") == "header2Value1,header2Value2"
                        && (requestContext.getHeaderString("Content-type") == MediaType.APPLICATION_JSON
                        || requestContext.getHeaderString("Content-type") == MediaType.APPLICATION_XML)) {

                        String bodyStr = (String) requestContext.getEntity()
                        ObjectValue body = Values.<ObjectValue> parse(bodyStr).orElse(null)
                        if (body.get("prop1").isPresent() && body.get("prop2").isPresent()) {
                            pingCount++
                            requestContext.abortWith(Response.ok().build())
                            return
                        }
                    }
                    break
                case "https://mockapi/put_request_with_headers":
                    UriInfo uriInfo = new ResteasyUriInfo(requestContext.uri)
                    def queryParams = uriInfo.getQueryParameters(true)
                    if (queryParams.get("param1").size() == 1
                        && queryParams.getFirst("param1") == "param1Value1"
                        && queryParams.get("param2").size() == 3
                        && queryParams.get("param2").get(0) == "param2Value1"
                        && queryParams.get("param2").get(1) == "param2Value2"
                        && queryParams.get("param2").get(2) == "param2Value3"
                        && queryParams.get("param3").size() == 1
                        && queryParams.getFirst("param3") == "param3Value1"
                        && requestContext.method == HttpMethod.PUT
                        && requestContext.getHeaderString("header1") == null
                        && requestContext.getHeaderString("header2") == "header2Value1,header2Value2"
                        && requestContext.getHeaderString("Content-type") == MediaType.APPLICATION_JSON) {

                        String bodyStr = (String) requestContext.getEntity()
                        ObjectValue body = Values.<ObjectValue> parse(bodyStr).orElse(null)
                        if (body.get("prop1").isPresent()
                            && body.get("prop1").get().toString() == /{"myProp1":123,"myProp2":true}/
                            && body.get("prop2").isPresent() && body.get("prop2").get().toString() == "prop2Value") {
                            putRequestWithHeadersCalled = true
                            requestContext.abortWith(Response.ok().build())
                            return
                        }
                    }
                    break
                case "https://mockapi/value/50/set":
                    requestContext.abortWith(Response.ok().build())
                    return
                case "https://mockapi/get_poll_slow":
                    pollCountSlow++
                    requestContext.abortWith(
                        Response
                            .ok("This is an example response where the value of 100% is in the body of the message.", MediaType.TEXT_PLAIN)
                            .build()
                    )
                    return
                case "https://mockapi/get_poll_fast":
                    pollCountFast++
                    requestContext.abortWith(
                        Response
                            .ok("This is an example response where there are multiple values of 100% 60% in the body of the message.", MediaType.TEXT_PLAIN)
                            .build()
                    )
                    return
                case "https://mockapi/get_success_200":
                case "https://redirected.mockapi/get_success_200":
                    successFailureCount++
                    requestContext.abortWith(Response.ok().build())
                    return
                case "https://mockapi/get_failure_401":
                    successFailureCount++
                    requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build())
                    return
                case "https://mockapi/redirect":
                    requestContext.abortWith(Response.temporaryRedirect(new URI("https://redirected.mockapi/get_success_200")).build())
                    return
            }

            requestContext.abortWith(Response.serverError().build())
        }
    }

    def cleanup() {
        mockServer.supportsRefresh = false
        mockServer.accessToken = null
        mockServer.refreshToken = null
        mockServer.accessTokenCount = 0
        mockServer.refreshTokenCount = 0
        mockServer.pingCount = 0
        mockServer.pollCountSlow = 0
        mockServer.pollCountFast = 0
        mockServer.successFailureCount = 0
        mockServer.putRequestWithHeadersCalled = false
    }

    def "Check UDP client protocol configuration and linked attribute deployment"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 1)

        and: "the UDP client protocol min times are adjusted for testing"
        UdpClientProtocol.MIN_POLLING_MILLIS = 10

        when: "the container starts"
        def serverPort = findEphemeralPort()
        def container = startContainerNoDemoImport(defaultConfig(serverPort), defaultServices())
        def protocolExecutorService = container.getService(ProtocolExecutorService.class)
        def udpClientProtocol = container.getService(UdpClientProtocol.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def agentService = container.getService(AgentService.class)

        then: "the container should be running"
        conditions.eventually {
            assert container.isRunning()
        }

        when: "a simple UDP echo server is started"
        def echoServerPort = findEphemeralPort()
        def clientPort = findEphemeralPort()
        def echoServer = new UdpStringServer(protocolExecutorService, new InetSocketAddress(echoServerPort), ";", Integer.MAX_VALUE, true)
        def doEcho = true
        def clientActualPort = null
        def lastCommand = null
        def receivedMessages = []
        echoServer.addMessageConsumer({
            message, channel, sender ->
                clientActualPort = sender.port
                lastCommand = message
                receivedMessages.add(message)

                if (doEcho) {
                    echoServer.sendMessage(message, sender)
                }
        })
        echoServer.start()

        then: "the UDP echo server should be connected"
        conditions.eventually {
            echoServer.connectionStatus == ConnectionStatus.CONNECTED
        }

        when: "an agent with a UDP client protocol configuration is created"
        def agent = new Asset()
        agent.setRealm(Constants.MASTER_REALM)
        agent.setName("Test Agent")
        agent.setType(AssetType.AGENT)
        agent.setAttributes(
            initProtocolConfiguration(new AssetAttribute("protocolConfig"), UdpClientProtocol.PROTOCOL_NAME)
                .addMeta(
                    new MetaItem(
                        UdpClientProtocol.META_PROTOCOL_HOST,
                        Values.create("255.255.255.255")
                    ),
                    new MetaItem(
                        UdpClientProtocol.META_PROTOCOL_PORT,
                        Values.create(echoServerPort)
                    ),
                    new MetaItem(
                        UdpClientProtocol.META_PROTOCOL_BIND_PORT,
                        Values.create(clientPort)
                    ),
                    new MetaItem(
                        UdpClientProtocol.META_RESPONSE_TIMEOUT_MILLIS,
                        Values.create(200)
                    )
                )
        )

        and: "the agent is added to the asset service"
        agent = assetStorageService.merge(agent)

        then: "the protocol should authenticate and start pinging the server and the connection status should become CONNECTED"
        conditions.eventually {
            def status = agentService.getProtocolConnectionStatus(new AttributeRef(agent.id, "protocolConfig"))
            assert status == ConnectionStatus.CONNECTED
        }

        when: "an asset is created with attributes linked to the protocol configuration"
        def asset = new Asset("Test Asset", AssetType.THING, agent)
        asset.setAttributes(
            // simple send with no retries
            new AssetAttribute("echoHello", AttributeValueType.STRING)
                .addMeta(
                    new MetaItem(MetaItemType.AGENT_LINK, new AttributeRef(agent.id, "protocolConfig").toArrayValue()),
                    new MetaItem(UdpClientProtocol.META_ATTRIBUTE_WRITE_VALUE, Values.create('Hello {$value};')),
                    new MetaItem(UdpClientProtocol.META_SEND_RETRIES, Values.create(0)),
                    new MetaItem(UdpClientProtocol.META_SERVER_ALWAYS_RESPONDS, Values.create(true))
                ),
            // attribute send with 10 retries
            new AssetAttribute("echoWorld", AttributeValueType.STRING)
                .addMeta(
                    new MetaItem(MetaItemType.AGENT_LINK, new AttributeRef(agent.id, "protocolConfig").toArrayValue()),
                    new MetaItem(UdpClientProtocol.META_ATTRIBUTE_WRITE_VALUE, Values.create("World;")),
                    new MetaItem(UdpClientProtocol.META_SEND_RETRIES, Values.create(10)),
                    new MetaItem(UdpClientProtocol.META_SERVER_ALWAYS_RESPONDS, Values.create(true))
                ),
            // attribute poll with 3 retries
            new AssetAttribute("pollTest", AttributeValueType.STRING)
                .addMeta(
                    new MetaItem(MetaItemType.AGENT_LINK, new AttributeRef(agent.id, "protocolConfig").toArrayValue()),
                    new MetaItem(UdpClientProtocol.META_ATTRIBUTE_WRITE_VALUE, Values.create("Test;")),
                    new MetaItem(UdpClientProtocol.META_POLLING_MILLIS, Values.create(100)),
                    new MetaItem(UdpClientProtocol.META_SEND_RETRIES, Values.create(3))
                ),
        )

        and: "the asset is merged into the asset service"
        asset = assetStorageService.merge(asset)

        then: "the polling attribute should have the correct values"
        def messageCount = 0
        conditions.eventually {
            asset = assetStorageService.find(asset.getId(), true)
            assert asset.getAttribute("pollTest").flatMap({ it.getValueAsString() }).orElse(null) == "Test"
            messageCount = receivedMessages.size()
        }

        and: "the attribute should continue sending to the server"
        conditions.eventually {
            receivedMessages.size() > messageCount + 1
        }

        when: "a linked attribute value is updated"
        def attributeEvent = new AttributeEvent(asset.id,
            "echoHello",
            Values.create("there"))
        assetProcessingService.sendAttributeEvent(attributeEvent)

        then: "the server should have received the request"
        conditions.eventually {
            assert receivedMessages.indexOf("Hello there") >= 0
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}