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
package org.openremote.test.protocol.http

import org.jboss.resteasy.spi.ResteasyUriInfo
import org.jboss.resteasy.util.BasicAuthHelper
import org.openremote.agent.protocol.Protocol
import org.openremote.container.util.Util
import org.openremote.agent.protocol.http.*
import org.openremote.container.web.OAuthGrant
import org.openremote.container.web.OAuthPasswordGrant
import org.openremote.container.web.OAuthRefreshTokenGrant
import org.openremote.container.web.OAuthServerResponse
import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService

import org.openremote.model.Constants
import org.openremote.model.asset.Asset
import org.openremote.model.asset.AssetAttribute
import org.openremote.model.attribute.MetaItemType
import org.openremote.model.asset.AssetType
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.AttributeRef
import org.openremote.model.attribute.AttributeValueType
import org.openremote.model.attribute.MetaItem
import org.openremote.model.value.ObjectValue
import org.openremote.model.value.RegexValueFilter
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

class HttpClientProtocolTest extends Specification implements ManagerContainerTrait {

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
        private String dynamicPathParam = ""

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

                        String bodyStr = (String)requestContext.getEntity()
                        ObjectValue body = Values.<ObjectValue>parse(bodyStr).orElse(null)
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

                        String bodyStr = (String)requestContext.getEntity()
                        ObjectValue body = Values.<ObjectValue>parse(bodyStr).orElse(null)
                        if (body.get("prop1").isPresent()
                            && body.get("prop1").get().toString() == /{"myProp1":123,"myProp2":true}/
                            && body.get("prop2").isPresent() && body.get("prop2").get().toString() == "prop2Value") {
                            putRequestWithHeadersCalled = true
                            requestContext.abortWith(Response.ok().build())
                            return
                        }
                    }
                    break
                case "https://mockapi/value/on/set":
                    dynamicPathParam = "on"
                    requestContext.abortWith(Response.ok().build())
                    return
                case "https://mockapi/value/off/set":
                    dynamicPathParam = "off"
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

    def "Check HTTP client protocol configuration and linked attribute deployment"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, initialDelay: 1)

        and: "the HTTP client protocol min times are adjusted for testing"
        HttpClientProtocol.MIN_POLLING_MILLIS = 10
        HttpClientProtocol.MIN_PING_MILLIS = 10

        when: "the container starts"
        def serverPort = findEphemeralPort()
        def container = startContainerNoDemoImport(defaultConfig(serverPort), defaultServices())
        def httpClientProtocol = container.getService(HttpClientProtocol.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def agentService = container.getService(AgentService.class)

        then: "the container should be running"
        conditions.eventually {
            assert container.isRunning()
        }

        when: "the web target builder is configured to use the mock server"
        if (!httpClientProtocol.client.configuration.isRegistered(mockServer)) {
            httpClientProtocol.client.register(mockServer, Integer.MAX_VALUE)
        }

        and: "an agent with a HTTP client protocol configuration is created"
        def agent = new Asset()
        agent.setRealm(Constants.MASTER_REALM)
        agent.setName("Test Agent")
        agent.setType(AssetType.AGENT)
        agent.setAttributes(
            initProtocolConfiguration(new AssetAttribute("protocolConfig"), HttpClientProtocol.PROTOCOL_NAME)
                .addMeta(
                    new MetaItem(
                        HttpClientProtocol.META_PROTOCOL_BASE_URI,
                        Values.create("https://mockapi")
                    ),
                    new MetaItem(
                        Protocol.META_PROTOCOL_OAUTH_GRANT,
                        new OAuthPasswordGrant("https://mockapi/token",
                            "TestClient",
                            "TestSecret",
                            "scope1 scope2",
                            "testuser",
                            "password").toObjectValue()
                    ),
                    new MetaItem(
                        HttpClientProtocol.META_PROTOCOL_FOLLOW_REDIRECTS,
                        Values.create(true)
                    ),
                    new MetaItem(
                        HttpClientProtocol.META_PROTOCOL_PING_PATH,
                        Values.create("pingGet")
                    ),
                    new MetaItem(
                        HttpClientProtocol.META_PROTOCOL_PING_MILLIS,
                        Values.create(100)
                    ),
                    new MetaItem(
                        HttpClientProtocol.META_HEADERS,
                        Values.<ObjectValue>parse(/{"header1": "header1Value1", "header2": ["header2Value1","header2Value2"]}/).get()
                    ),
                    new MetaItem(
                        HttpClientProtocol.META_QUERY_PARAMETERS,
                        Values.<ObjectValue>parse(/{"param1": "param1Value1", "param2": ["param2Value1","param2Value2"]}/).get()
                    )
                )
        )

        and: "the agent is added to the asset service"
        agent = assetStorageService.merge(agent)

        then: "the protocol should authenticate and start pinging the server and the connection status should become CONNECTED"
        conditions.eventually {
            def status = agentService.getProtocolConnectionStatus(new AttributeRef(agent.id, "protocolConfig"))
            assert status == ConnectionStatus.CONNECTED
            assert mockServer.pingCount > 2
        }

        when: "the protocol configuration is removed"
        agent.removeAttribute("protocolConfig")
        agent = assetStorageService.merge(agent)

        then: "the request client should be removed"
        conditions.eventually {
            assert httpClientProtocol.clientMap.isEmpty()
            assert httpClientProtocol.requestMap.isEmpty()
            assert httpClientProtocol.pollingMap.isEmpty()
        }

        and: "ping polling should have stopped"
        def pingCount = mockServer.pingCount
        Thread.sleep(50)
        assert mockServer.pingCount == pingCount

        // Test ping using POST with body and query parameters

        when: "ping count is reset"
        mockServer.pingCount = 0

        and: "a protocol configuration is added to the agent that uses POST ping mechanism"
        agent.addAttributes(
            initProtocolConfiguration(new AssetAttribute("protocolConfig"), HttpClientProtocol.PROTOCOL_NAME)
                .addMeta(
                new MetaItem(
                    HttpClientProtocol.META_PROTOCOL_BASE_URI,
                    Values.create("https://mockapi")
                ),
                new MetaItem(
                    Protocol.META_PROTOCOL_OAUTH_GRANT,
                    new OAuthPasswordGrant("https://mockapi/token",
                        "TestClient",
                        "TestSecret",
                        "scope1 scope2",
                        "testuser",
                        "password").toObjectValue()
                ),
                new MetaItem(
                    HttpClientProtocol.META_PROTOCOL_FOLLOW_REDIRECTS,
                    Values.create(true)
                ),
                new MetaItem(
                    HttpClientProtocol.META_HEADERS,
                    Values.<ObjectValue>parse(/{"header1": "header1Value1", "header2": ["header2Value1","header2Value2"]}/).get()
                ),
                new MetaItem(
                    HttpClientProtocol.META_QUERY_PARAMETERS,
                    Values.<ObjectValue>parse(/{"param1": "param1Value1", "param2": ["param2Value1","param2Value2"]}/).get()
                ),
                new MetaItem(
                    HttpClientProtocol.META_PROTOCOL_PING_PATH,
                    Values.create("pingPost")
                ),
                new MetaItem(
                    HttpClientProtocol.META_PROTOCOL_PING_CONTENT_TYPE,
                    Values.create(MediaType.APPLICATION_JSON)
                ),
                new MetaItem(
                    HttpClientProtocol.META_PROTOCOL_PING_MILLIS,
                    Values.create(100)
                ),
                new MetaItem(
                    HttpClientProtocol.META_PROTOCOL_PING_METHOD,
                    Values.create(HttpMethod.POST)
                ),
                new MetaItem(
                    HttpClientProtocol.META_PROTOCOL_PING_BODY,
                    Values.<ObjectValue>parse(/{"prop1": "prop1Value", "prop2": "prop2Value"}/).get()
                ),
                new MetaItem(
                    HttpClientProtocol.META_PROTOCOL_PING_QUERY_PARAMETERS,
                    Values.<ObjectValue>parse(/{"param1": "param1Value2", "param3": "param3Value1"}/).get()
                ),
            )
        )
        agent = assetStorageService.merge(agent)

        then: "the protocol should authenticate and start pinging the server and the connection status should become CONNECTED"
        conditions.eventually {
            def status = agentService.getProtocolConnectionStatus(new AttributeRef(agent.id, "protocolConfig"))
            assert status == ConnectionStatus.CONNECTED
            assert mockServer.pingCount > 2
        }

        when: "the ping content type is manually configured"
        agent.getAttribute("protocolConfig").ifPresent{
            it.addMeta(
                new MetaItem(
                    HttpClientProtocol.META_PROTOCOL_PING_CONTENT_TYPE,
                    Values.create(MediaType.APPLICATION_XML)
                )
            )}

        and: "the agent is merged to the asset service"
        def currentClient = httpClientProtocol.clientMap.values().first()
        agent = assetStorageService.merge(agent)

        then: "the client should eventually be updated"
        conditions.eventually {
            assert httpClientProtocol.clientMap.size() == 1
            assert httpClientProtocol.clientMap.values().first() != currentClient
        }

        and: "the protocol should authenticate and start pinging the server and the connection status should become CONNECTED"
        conditions.eventually {
            def status = agentService.getProtocolConnectionStatus(new AttributeRef(agent.id, "protocolConfig"))
            assert status == ConnectionStatus.CONNECTED
            assert mockServer.pingCount > 2
        }

        when: "an asset is created with attributes linked to the http protocol configuration"
        def asset = new Asset("Test Asset", AssetType.THING, agent)
        asset.setAttributes(
            // attribute that sends requests to the server using PUT with dynamic body and custom header to override parent
            new AssetAttribute("putRequestWithHeaders", AttributeValueType.OBJECT)
                .addMeta(
                    new MetaItem(MetaItemType.AGENT_LINK, new AttributeRef(agent.id, "protocolConfig").toArrayValue()),
                    new MetaItem(HttpClientProtocol.META_ATTRIBUTE_PATH, Values.create("put_request_with_headers")),
                    new MetaItem(HttpClientProtocol.META_ATTRIBUTE_METHOD, Values.create(HttpMethod.PUT)),
                    new MetaItem(
                        Protocol.META_ATTRIBUTE_WRITE_VALUE,
                        Values.create('{"prop1": {$value}, "prop2": "prop2Value"}')
                    ),
                    new MetaItem(
                        HttpClientProtocol.META_ATTRIBUTE_CONTENT_TYPE,
                        Values.create(MediaType.APPLICATION_JSON)
                    ),
                    new MetaItem(
                        HttpClientProtocol.META_HEADERS,
                        Values.createObject().put("header1", (Value)null)
                    ),
                    new MetaItem(
                        HttpClientProtocol.META_QUERY_PARAMETERS,
                        Values.<ObjectValue>parse(/{"param2": "param2Value3", "param3": "param3Value1"}/).get()
                    )
                ),
            // attribute that sends requests to the server using GET with dynamic path
            new AssetAttribute("getRequestWithDynamicPath", AttributeValueType.BOOLEAN)
                .addMeta(
                    new MetaItem(MetaItemType.AGENT_LINK, new AttributeRef(agent.id, "protocolConfig").toArrayValue()),
                    new MetaItem(HttpClientProtocol.META_ATTRIBUTE_PATH, Values.create('value/{$value}/set')),
                    new MetaItem(Protocol.META_ATTRIBUTE_WRITE_VALUE_CONVERTER, Values.parse("{\n" +
                        "    \"TRUE\": \"on\",\n" +
                        "    \"FALSE\": \"off\"\n" +
                        "}").get())
                ),
            // attribute that polls the server using GET and uses regex filter on response
            new AssetAttribute("getPollSlow", AttributeValueType.NUMBER)
                .addMeta(
                    new MetaItem(MetaItemType.AGENT_LINK, new AttributeRef(agent.id, "protocolConfig").toArrayValue()),
                    new MetaItem(HttpClientProtocol.META_ATTRIBUTE_PATH, Values.create("get_poll_slow")),
                    new MetaItem(HttpClientProtocol.META_ATTRIBUTE_POLLING_MILLIS, Values.create(50)), // This is ms in testing
                    new MetaItem(
                        Protocol.META_ATTRIBUTE_VALUE_FILTERS,
                        Values.createArray().add(Util.objectToValue(new RegexValueFilter("\\d+", 0, 0)).get())
                    )
                ),
            // attribute that polls the server using GET and uses regex filter on response
            new AssetAttribute("getPollFast", AttributeValueType.NUMBER)
                .addMeta(
                    new MetaItem(MetaItemType.AGENT_LINK, new AttributeRef(agent.id, "protocolConfig").toArrayValue()),
                    new MetaItem(HttpClientProtocol.META_ATTRIBUTE_PATH, Values.create("get_poll_fast")),
                    new MetaItem(HttpClientProtocol.META_ATTRIBUTE_POLLING_MILLIS, Values.create(40)), // This is ms in testing
                    new MetaItem(
                        Protocol.META_ATTRIBUTE_VALUE_FILTERS,
                        Values.createArray().add(Util.objectToValue(new RegexValueFilter("\\d+", 0, 1)).get())
                    )
                )
        )

        and: "the asset is merged into the asset service"
        asset = assetStorageService.merge(asset)
        def requestCount = httpClientProtocol.requestMap.size()

        then: "new request maps should be created in the HTTP client protocol for the linked attributes"
        conditions.eventually {
            assert httpClientProtocol.requestMap.size() == requestCount + 4
        }

        and: "the polling attributes should be polling the server"
        conditions.eventually {
            assert mockServer.pollCountSlow > 0
            assert mockServer.pollCountFast > 0
            assert mockServer.pollCountFast > mockServer.pollCountSlow
        }

        and: "the polling attributes should have the correct values"
        conditions.eventually {
            asset = assetStorageService.find(asset.getId(), true)
            assert asset.getAttribute("getPollSlow").flatMap({it.getValueAsInteger()}).orElse(null) == 100
            assert asset.getAttribute("getPollFast").flatMap({it.getValueAsInteger()}).orElse(null) == 60
        }

        when: "a linked attribute value is updated"
        def attributeEvent = new AttributeEvent(asset.id,
            "putRequestWithHeaders",
            Values.<ObjectValue>parse('{"myProp1": 123,"myProp2": true}').get())
        assetProcessingService.sendAttributeEvent(attributeEvent)

        then: "the server should have received the request"
        conditions.eventually {
            assert mockServer.putRequestWithHeadersCalled
        }

        when: "a linked attribute value is updated to true the path should contain the dynamic mapped value of 'on'"
        def count = mockServer.successFailureCount
        attributeEvent = new AttributeEvent(asset.id,
            "getRequestWithDynamicPath",
            Values.create(true))
        assetProcessingService.sendAttributeEvent(attributeEvent)

        then: "the server should have received the request and returned a 200"
        conditions.eventually {
            assert mockServer.dynamicPathParam == "on"
        }

        when: "a linked attribute value is updated to false the path should contain the dynamic mapped value of 'off'"
        count = mockServer.successFailureCount
        attributeEvent = new AttributeEvent(asset.id,
            "getRequestWithDynamicPath",
            Values.create(false))
        assetProcessingService.sendAttributeEvent(attributeEvent)

        then: "the server should have received the request and returned a 200"
        conditions.eventually {
            assert mockServer.dynamicPathParam == "off"
        }

        when: "a protocol configurations are added to the agent that don't use ping mechanism"
        agent.addAttributes(
            initProtocolConfiguration(new AssetAttribute("protocolConfig2"), HttpClientProtocol.PROTOCOL_NAME)
                .addMeta(
                new MetaItem(
                    HttpClientProtocol.META_PROTOCOL_BASE_URI,
                    Values.create("https://mockapi")
                ),
                new MetaItem(
                    Protocol.META_PROTOCOL_OAUTH_GRANT,
                    new OAuthPasswordGrant("https://mockapi/token",
                        "TestClient",
                        "TestSecret",
                        "scope1 scope2",
                        "testuser",
                        "password").toObjectValue()
                ),
                new MetaItem(
                    HttpClientProtocol.META_FAILURE_CODES,
                    Values.createArray()
                        .add(Values.create(Response.Status.UNAUTHORIZED.statusCode))
                        .add(Values.create(Response.Status.FORBIDDEN.statusCode))
                )
            ),
            initProtocolConfiguration(new AssetAttribute("protocolConfig3"), HttpClientProtocol.PROTOCOL_NAME)
                .addMeta(
                new MetaItem(
                    HttpClientProtocol.META_PROTOCOL_BASE_URI,
                    Values.create("https://mockapi")
                ),
                new MetaItem(
                    Protocol.META_PROTOCOL_OAUTH_GRANT,
                    new OAuthPasswordGrant("https://mockapi/token",
                        "TestClient",
                        "TestSecret",
                        "scope1 scope2",
                        "testuser",
                        "password").toObjectValue()
                ),
                new MetaItem(
                    HttpClientProtocol.META_FAILURE_CODES,
                    Values.createArray()
                        .add(Values.create(Response.Status.UNAUTHORIZED.statusCode))
                        .add(Values.create(Response.Status.FORBIDDEN.statusCode))
                )
            ),
            initProtocolConfiguration(new AssetAttribute("protocolConfig4"), HttpClientProtocol.PROTOCOL_NAME)
                .addMeta(
                new MetaItem(
                    HttpClientProtocol.META_PROTOCOL_BASE_URI,
                    Values.create("https://mockapi")
                ),
                new MetaItem(
                    Protocol.META_PROTOCOL_OAUTH_GRANT,
                    new OAuthPasswordGrant("https://mockapi/token",
                        "TestClient",
                        "TestSecret",
                        "scope1 scope2",
                        "testuser",
                        "password").toObjectValue()
                ),
                new MetaItem(
                    HttpClientProtocol.META_PROTOCOL_FOLLOW_REDIRECTS,
                    Values.create(true)
                )
            )
        )
        def protocolCount = httpClientProtocol.linkedProtocolConfigurations.size()
        agent = assetStorageService.merge(agent)

        then: "the protocol configuration should be linked with a connection status of connected"
        conditions.eventually {
            assert httpClientProtocol.linkedProtocolConfigurations.size() == protocolCount + 3
            assert agentService.getProtocolConnectionStatus(new AttributeRef(agent.id, "protocolConfig2")) == ConnectionStatus.CONNECTED
            assert agentService.getProtocolConnectionStatus(new AttributeRef(agent.id, "protocolConfig3")) == ConnectionStatus.CONNECTED
            assert agentService.getProtocolConnectionStatus(new AttributeRef(agent.id, "protocolConfig4")) == ConnectionStatus.CONNECTED
        }

        when: "attributes are linked to these protocol configurations"
        def asset2 = new Asset("Test Asset 2", AssetType.THING, agent)
        asset2.addAttributes(
            new AssetAttribute("getSuccess", AttributeValueType.STRING)
                .addMeta(
                    new MetaItem(MetaItemType.AGENT_LINK, new AttributeRef(agent.id, "protocolConfig2").toArrayValue()),
                    new MetaItem(HttpClientProtocol.META_ATTRIBUTE_PATH, Values.create("get_success_200"))
                ),
            new AssetAttribute("getFailure", AttributeValueType.STRING)
                .addMeta(
                    new MetaItem(MetaItemType.AGENT_LINK, new AttributeRef(agent.id, "protocolConfig2").toArrayValue()),
                    new MetaItem(HttpClientProtocol.META_ATTRIBUTE_PATH, Values.create("get_failure_401"))
                ),
            new AssetAttribute("pollFailure", AttributeValueType.STRING)
                .addMeta(
                    new MetaItem(MetaItemType.AGENT_LINK, new AttributeRef(agent.id, "protocolConfig3").toArrayValue()),
                    new MetaItem(HttpClientProtocol.META_ATTRIBUTE_PATH, Values.create("get_failure_401")),
                    new MetaItem(HttpClientProtocol.META_ATTRIBUTE_POLLING_MILLIS, Values.create(50)), // This is ms in testing
                ),
            new AssetAttribute("getSuccess2", AttributeValueType.STRING)
                .addMeta(
                new MetaItem(MetaItemType.AGENT_LINK, new AttributeRef(agent.id, "protocolConfig4").toArrayValue()),
                new MetaItem(HttpClientProtocol.META_ATTRIBUTE_PATH, Values.create("get_success_200"))
            ),
            new AssetAttribute("getFailure2", AttributeValueType.STRING)
                .addMeta(
                new MetaItem(MetaItemType.AGENT_LINK, new AttributeRef(agent.id, "protocolConfig4").toArrayValue()),
                new MetaItem(HttpClientProtocol.META_ATTRIBUTE_PATH, Values.create("get_failure_401"))
            ),
            new AssetAttribute("getRedirect", AttributeValueType.STRING)
                .addMeta(
                new MetaItem(MetaItemType.AGENT_LINK, new AttributeRef(agent.id, "protocolConfig4").toArrayValue()),
                new MetaItem(HttpClientProtocol.META_ATTRIBUTE_PATH, Values.create("redirect"))
            ),
        )

        and: "the asset is merged into the asset service"
        requestCount = httpClientProtocol.requestMap.size()
        asset2 = assetStorageService.merge(asset2)

        then: "new request maps should be created in the HTTP client protocol for the linked attributes"
        conditions.eventually {
            assert httpClientProtocol.requestMap.size() == requestCount + 5
        }

        and: "the protocol config connection status linked to by the failing poll request should become DISABLED"
        conditions.eventually {
            assert agentService.getProtocolConnectionStatus(new AttributeRef(agent.id, "protocolConfig3")) == ConnectionStatus.DISABLED
        }

        when: "the success/failure count is stored"
        count = mockServer.successFailureCount

        then: "the poll request will no longer reach the server (the success/failure count shouldn't change)"
        Thread.sleep(50)
        assert mockServer.successFailureCount == count

        when: "a linked attribute value is updated"
        attributeEvent = new AttributeEvent(asset2.id,
            "getSuccess",
            Values.create("OK"))
        assetProcessingService.sendAttributeEvent(attributeEvent)

        then: "the server should have received the request"
        conditions.eventually {
            assert mockServer.successFailureCount == count + 1
        }

        and: "the protocol connection status should now be CONNECTED"
        conditions.eventually {
            assert agentService.getProtocolConnectionStatus(new AttributeRef(agent.id, "protocolConfig2")) == ConnectionStatus.CONNECTED
        }

        when: "a linked attribute value is updated"
        count = mockServer.successFailureCount
        attributeEvent = new AttributeEvent(asset2.id,
            "getFailure",
            Values.create("OK"))
        assetProcessingService.sendAttributeEvent(attributeEvent)

        then: "the server should have received the request and returned a 401"
        conditions.eventually {
            assert mockServer.successFailureCount == count + 1
        }

        and: "the protocol connection status should now be DISABLED"
        conditions.eventually {
            assert agentService.getProtocolConnectionStatus(new AttributeRef(agent.id, "protocolConfig2")) == ConnectionStatus.DISABLED
        }

        when: "a linked attribute value is updated again"
        count = mockServer.successFailureCount
        attributeEvent = new AttributeEvent(asset2.id,
            "getSuccess",
            Values.create("OK"))
        assetProcessingService.sendAttributeEvent(attributeEvent)

        then: "the server should not have received the request"
        Thread.sleep(50)
        assert mockServer.successFailureCount == count

        when: "a linked attribute value is updated"
        attributeEvent = new AttributeEvent(asset2.id,
            "getFailure2",
            Values.create("OK"))
        assetProcessingService.sendAttributeEvent(attributeEvent)

        then: "the server should have received the request and returned a 401"
        conditions.eventually {
            assert mockServer.successFailureCount == count + 1
        }

        and: "the protocol connection status should now be ERROR_AUTHENTICATION"
        conditions.eventually {
            assert agentService.getProtocolConnectionStatus(new AttributeRef(agent.id, "protocolConfig4")) == ConnectionStatus.ERROR_AUTHENTICATION
        }

        when: "a linked attribute value is updated"
        count = mockServer.successFailureCount
        attributeEvent = new AttributeEvent(asset2.id,
            "getSuccess2",
            Values.create("OK"))
        assetProcessingService.sendAttributeEvent(attributeEvent)

        then: "the server should have received the request and returned a 200"
        conditions.eventually {
            assert mockServer.successFailureCount == count + 1
        }

        and: "the protocol connection status should now be CONNECTED"
        conditions.eventually {
            assert agentService.getProtocolConnectionStatus(new AttributeRef(agent.id, "protocolConfig4")) == ConnectionStatus.CONNECTED
        }

        when: "a linked attribute value is updated"
        count = mockServer.successFailureCount
        attributeEvent = new AttributeEvent(asset2.id,
            "getRedirect",
            Values.create("OK"))
        assetProcessingService.sendAttributeEvent(attributeEvent)

        then: "the server should have sent a redirect request and the redirected endpoint returned a 200"
        conditions.eventually {
            assert mockServer.successFailureCount == count + 1
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
