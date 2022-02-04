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

import com.fasterxml.jackson.databind.node.ObjectNode
import org.jboss.resteasy.spi.ResteasyUriInfo
import org.jboss.resteasy.util.BasicAuthHelper
import org.openremote.agent.protocol.http.HTTPAgent
import org.openremote.agent.protocol.http.HTTPAgentLink
import org.openremote.agent.protocol.http.HTTPMethod
import org.openremote.agent.protocol.http.HTTPProtocol
import org.openremote.container.web.OAuthServerResponse
import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.model.Constants
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.MetaItem
import org.openremote.model.auth.OAuthGrant
import org.openremote.model.auth.OAuthPasswordGrant
import org.openremote.model.auth.OAuthRefreshTokenGrant
import org.openremote.model.util.ValueUtil
import org.openremote.model.value.RegexValueFilter
import org.openremote.model.value.SubStringValueFilter
import org.openremote.model.value.ValueFilter
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import javax.ws.rs.HttpMethod
import javax.ws.rs.client.ClientRequestContext
import javax.ws.rs.client.ClientRequestFilter
import javax.ws.rs.core.*
import java.util.regex.Pattern

import static org.openremote.model.value.MetaItemType.AGENT_LINK
import static org.openremote.model.value.ValueType.*

class HttpClientProtocolTest extends Specification implements ManagerContainerTrait {

    @Shared
    def mockServer = new ClientRequestFilter() {

        private boolean supportsRefresh
        private int accessTokenCount
        private int refreshTokenCount
        private String accessToken = null
        private String refreshToken = null
        private int pollCountFast = 0
        private int pollCountSlow = 0
        private boolean putRequestWithHeadersCalled = false
        private int successCount = 0
        private int failureCount = 0
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
                        && grant.getFirst(OAuthRefreshTokenGrant.REFRESH_TOKEN_GRANT_TYPE) == refreshToken) {
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
                case "https://mockapi/put_request_with_headers":
                    UriInfo uriInfo = new ResteasyUriInfo(requestContext.uri)
                    def queryParams = uriInfo.getQueryParameters(true)
                    if (queryParams.get("param1").size() == 1
                        && queryParams.getFirst("param1") == "param1Value1"
                        && queryParams.get("param2").size() == 3
                        && queryParams.get("param2").contains("param2Value1")
                        && queryParams.get("param2").contains("param2Value2")
                        && queryParams.get("param2").contains("param2Value3")
                        && queryParams.get("param3").size() == 1
                        && queryParams.getFirst("param3") == "param3Value1"
                        && requestContext.method == HttpMethod.PUT
                        && requestContext.getHeaderString("header1") == "header1Value1"
                        && requestContext.getHeaderString("header2") == "header2Value1,header2Value2"
                        && requestContext.getHeaderString("header3") == "header3Value1,header3Value2"
                        && requestContext.getHeaderString("Content-type") == MediaType.APPLICATION_JSON) {

                        String bodyStr = (String)requestContext.getEntity()
                        ObjectNode body = ValueUtil.parse(bodyStr).orElse(null)
                        if (body.has("prop1")
                            && body.get("prop1").get("myProp1").asInt() == 123
                            && body.get("prop1").get("myProp2").asBoolean()
                            && body.has("prop2") && body.get("prop2").asText() == "prop2Value") {
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
                    successCount++
                    requestContext.abortWith(Response.ok().build())
                    return
                case "https://mockapi/get_failure_401":
                    failureCount++
                    requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build())
                    return
                case "https://mockapi/redirect":
                    requestContext.abortWith(Response.temporaryRedirect(new URI("https://redirected.mockapi/get_success_200")).build())
                    return
                case "https://mockapi/binary":
                    requestContext.abortWith(
                            Response.ok(
                                    new byte[] {(byte)0x1F & 0xFF, (byte)0x56 & 0xFF, (byte)0x01 & 0xFF, (byte)0xAE & 0xFF, (byte)0x12 & 0xFF}, MediaType.APPLICATION_OCTET_STREAM_TYPE
                            ).build()
                    )
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
        mockServer.pollCountSlow = 0
        mockServer.pollCountFast = 0
        mockServer.successCount = 0
        mockServer.failureCount = 0
        mockServer.putRequestWithHeadersCalled = false
    }

    def "Check HTTP client protocol and linked attribute deployment"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, initialDelay: 1)

        and: "the HTTP client protocol min times are adjusted for testing"
        HTTPProtocol.MIN_POLLING_MILLIS = 10

        and: "the container starts"
        def container = startContainer(defaultConfig(), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def agentService = container.getService(AgentService.class)

        when: "the web target builder is configured to use the mock server"
        if (!HTTPProtocol.client.configuration.isRegistered(mockServer)) {
            HTTPProtocol.client.register(mockServer, Integer.MAX_VALUE)
        }

        and: "a HTTP client agent is created"
        HTTPAgent agent = new HTTPAgent("Test agent")
            .setRealm(Constants.MASTER_REALM)
            .setBaseURI("https://mockapi")
            .setOAuthGrant(
                new OAuthPasswordGrant("https://mockapi/token",
                    "TestClient",
                    "TestSecret",
                    "scope1 scope2",
                    "testuser",
                    "password")
            )
        .setFollowRedirects(true)
        .setRequestHeaders(
            ValueUtil.parse(/{"header1": ["header1Value1"], "header2": ["header2Value1","header2Value2"]}/, MultivaluedStringMap.class).orElseThrow()
        )
        .setRequestQueryParameters(
            ValueUtil.parse(/{"param1": ["param1Value1"], "param2": ["param2Value1","param2Value2"]}/, MultivaluedStringMap.class).orElseThrow()
        )

        and: "the agent is added to the asset service"
        agent = assetStorageService.merge(agent)

        then: "the protocol should authenticate and the connection status should become CONNECTED"
        conditions.eventually {
            agent = assetStorageService.find(agent.id, HTTPAgent.class)
            assert agent.getAgentStatus().orElse(ConnectionStatus.DISCONNECTED) == ConnectionStatus.CONNECTED
        }

        when: "an asset is created with attributes linked to the agent"
        def asset = new ThingAsset("Test Asset")
            .setParent(agent)
            .addOrReplaceAttributes(
                // attribute that sends requests to the server using PUT with dynamic body and custom header to override parent
                new Attribute<>("putRequestWithHeaders", JSON_OBJECT)
                    .addMeta(
                        new MetaItem<>(AGENT_LINK, new HTTPAgentLink(agent.id)
                            .setPath("put_request_with_headers")
                            .setMethod(HTTPMethod.PUT)
                            .setWriteValue('{"prop1": {$value}, "prop2": "prop2Value"}')
                            .setContentType(MediaType.APPLICATION_JSON)
                            .setHeaders(new MultivaluedStringMap(
                                [
                                    ("header3") : ["header3Value1","header3Value2"]
                                ]
                            ))
                            .setQueryParameters(new MultivaluedStringMap(
                                [
                                    ("param2") : ["param2Value3"],
                                    ("param3") : ["param3Value1"]
                                ]
                            ))
                        )
                    ),
                // attribute that sends requests to the server using GET with dynamic path
                new Attribute<>("getRequestWithDynamicPath", BOOLEAN)
                    .addMeta(
                        new MetaItem<>(AGENT_LINK, new HTTPAgentLink(agent.id)
                            .setPath('value/{$value}/set')
                            .setWriteValueConverter((ObjectNode)ValueUtil.parse("{\n" +
                                "    \"TRUE\": \"on\",\n" +
                                "    \"FALSE\": \"off\"\n" +
                                "}").get())
                        )
                    ),
                // attribute that polls the server using GET and uses regex filter on response
                new Attribute<>("getPollSlow", INTEGER)
                    .addMeta(
                        new MetaItem<>(AGENT_LINK, new HTTPAgentLink(agent.id)
                            .setPath("get_poll_slow")
                        .setPollingMillis(200)
                            .setValueFilters(
                                [
                                    new RegexValueFilter(Pattern.compile("\\d+"))
                                ] as ValueFilter[]
                            )
                        )
                    ),
                // attribute that polls the server using GET and uses regex filter on response
                new Attribute<>("getPollFast", INTEGER)
                    .addMeta(
                        new MetaItem<>(AGENT_LINK, new HTTPAgentLink(agent.id)
                            .setPath("get_poll_fast")
                            .setPollingMillis(100)
                            .setValueFilters(
                                [
                                    new RegexValueFilter(Pattern.compile("\\d+")).setMatchIndex(1)
                                ] as ValueFilter[]
                            )
                        )
                    ),
                // attribute that expects binary data using GET and uses sub string on response
                new Attribute<>("getBinary", TEXT)
                    .addMeta(
                        new MetaItem<>(AGENT_LINK, new HTTPAgentLink(agent.id)
                            .setPath("binary")
                            .setPollingMillis(100)
                            .setMessageConvertBinary(true)
                            .setValueFilters(
                                [
                                    new SubStringValueFilter(22, 24)
                                ] as ValueFilter[]
                            )
                            .setValueConverter(ValueUtil.JSON.createObjectNode()
                                    .put("00", "OFF")
                                    .put("01", "ON")
                            )
                        )
                    ),
                // attribute that expects HEX data using GET and uses sub string on response
                new Attribute<>("getHex", TEXT)
                    .addMeta(
                        new MetaItem<>(AGENT_LINK, new HTTPAgentLink(agent.id)
                            .setPath("binary")
                            .setPollingMillis(100)
                            .setMessageConvertHex(true)
                            .setValueFilters(
                                [
                                    new SubStringValueFilter(4, 6)
                                ] as ValueFilter[]
                            )
                            .setValueConverter(ValueUtil.JSON.createObjectNode()
                                    .put("00", "OFF")
                                    .put("01", "ON")
                            )
                        )
                    )
        )

        and: "the asset is merged into the asset service"
        asset = assetStorageService.merge(asset)

        then: "new request maps should be created in the HTTP client protocol for the linked attributes"
        conditions.eventually {
            assert ((HTTPProtocol)agentService.getProtocolInstance(agent.id)).requestMap.size() == 6
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
            assert asset.getAttribute("getPollSlow").flatMap({it.value}).orElse(null) == 100
            assert asset.getAttribute("getPollFast").flatMap({it.value}).orElse(null) == 60
            assert asset.getAttribute("getBinary").flatMap({it.value}).orElse(null) == "ON"
            assert asset.getAttribute("getHex").flatMap({it.value}).orElse(null) == "ON"
        }

        when: "a linked attribute value is updated"
        def attributeEvent = new AttributeEvent(asset.id,
            "putRequestWithHeaders",
                ValueUtil.parse('{"myProp1": 123,"myProp2": true}').get())
        assetProcessingService.sendAttributeEvent(attributeEvent)

        then: "the server should have received the request"
        conditions.eventually {
            assert mockServer.putRequestWithHeadersCalled
        }

        when: "a linked attribute value is updated to true, the path should contain the dynamic mapped value of 'on'"
        attributeEvent = new AttributeEvent(asset.id,
            "getRequestWithDynamicPath",
            true)
        assetProcessingService.sendAttributeEvent(attributeEvent)

        then: "the server should have received the request and returned a 200"
        conditions.eventually {
            assert mockServer.dynamicPathParam == "on"
        }

        when: "a linked attribute value is updated to false the path should contain the dynamic mapped value of 'off'"
        attributeEvent = new AttributeEvent(asset.id,
            "getRequestWithDynamicPath",
            false)
        assetProcessingService.sendAttributeEvent(attributeEvent)

        then: "the server should have received the request and returned a 200"
        conditions.eventually {
            assert mockServer.dynamicPathParam == "off"
        }

        when: "new agents are created"
        def agent2 = agent
            .setId(null)
            .setFollowRedirects(null)
            .setRequestHeaders(null)
            .setRequestQueryParameters(null)
        def agent3 = ValueUtil.clone(agent2)
            .setId(null)
        def agent4 = ValueUtil.clone(agent2)
            .setId(null)
            .setFollowRedirects(true)

        agent2 = assetStorageService.merge(agent2)
        agent3 = assetStorageService.merge(agent3)
        agent4 = assetStorageService.merge(agent4)

        then: "the protocol instances should be created and the agents should be connected"
        conditions.eventually {
            assert agentService.getProtocolInstance(agent2.id) != null
            assert agentService.getProtocolInstance(agent3.id) != null
            assert agentService.getProtocolInstance(agent4.id) != null
            assert agentService.getAgent(agent2.id).getAgentStatus().orElse(null) == ConnectionStatus.CONNECTED
            assert agentService.getAgent(agent3.id).getAgentStatus().orElse(null) == ConnectionStatus.CONNECTED
            assert agentService.getAgent(agent4.id).getAgentStatus().orElse(null) == ConnectionStatus.CONNECTED
        }

        when: "attributes are linked to these agents"
        def asset2 = new ThingAsset("Test Asset 2")
            .setRealm(Constants.MASTER_REALM)
            .addOrReplaceAttributes(
                new Attribute<>("getSuccess", TEXT)
                    .addMeta(
                        new MetaItem<>(AGENT_LINK, new HTTPAgentLink(agent2.id)
                            .setPath("get_success_200")
                        )
                    ),
                new Attribute<>("getFailure", TEXT)
                    .addMeta(
                        new MetaItem<>(AGENT_LINK, new HTTPAgentLink(agent2.id)
                            .setPath("get_failure_401")
                        )
                    ),
                new Attribute<>("pollFailure", TEXT)
                    .addMeta(
                        new MetaItem<>(AGENT_LINK, new HTTPAgentLink(agent3.id)
                            .setPath("get_failure_401")
                            .setPollingMillis(100)
                        )
                    ),
                new Attribute<>("getSuccess2", TEXT)
                    .addMeta(
                        new MetaItem<>(AGENT_LINK, new HTTPAgentLink(agent4.id)
                            .setPath("get_success_200")
                        )
                    ),
                new Attribute<>("getFailure2", TEXT)
                    .addMeta(
                        new MetaItem<>(AGENT_LINK, new HTTPAgentLink(agent4.id)
                            .setPath("get_failure_401")
                        )
                    ),
                new Attribute<>("getRedirect", TEXT)
                    .addMeta(
                        new MetaItem<>(AGENT_LINK, new HTTPAgentLink(agent4.id)
                            .setPath("redirect")
                        )
                    )
            )

        and: "the asset is merged into the asset service"
        asset2 = assetStorageService.merge(asset2)

        then: "new request maps should be created in the HTTP client protocol for the linked attributes"
        conditions.eventually {
            assert ((HTTPProtocol)agentService.getProtocolInstance(agent2.id)) != null
            assert ((HTTPProtocol)agentService.getProtocolInstance(agent2.id)).requestMap.size() == 2
            assert ((HTTPProtocol)agentService.getProtocolInstance(agent3.id)) != null
            assert ((HTTPProtocol)agentService.getProtocolInstance(agent3.id)).requestMap.size() == 1
            assert ((HTTPProtocol)agentService.getProtocolInstance(agent4.id)) != null
            assert ((HTTPProtocol)agentService.getProtocolInstance(agent4.id)).requestMap.size() == 3
        }

        and: "the polling attribute should be polling the server"
        conditions.eventually {
            assert mockServer.failureCount > 1
        }

        when: "a linked attribute value is updated"
        attributeEvent = new AttributeEvent(asset2.id,
            "getSuccess",
            "OK")
        assetProcessingService.sendAttributeEvent(attributeEvent)

        then: "the server should have received the request"
        conditions.eventually {
            assert mockServer.successCount == 1
        }
    }
}
