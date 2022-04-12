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
import io.undertow.servlet.Servlets
import io.undertow.servlet.api.DeploymentManager
import org.jboss.resteasy.spi.ResteasyUriInfo
import org.jboss.resteasy.util.BasicAuthHelper
import org.openremote.agent.protocol.http.AbstractHTTPServerProtocol
import org.openremote.container.util.UniqueIdentifierGenerator
import org.openremote.container.web.OAuthServerResponse
import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.model.asset.Asset
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.auth.OAuthGrant
import org.openremote.model.auth.OAuthPasswordGrant
import org.openremote.model.auth.OAuthRefreshTokenGrant
import org.openremote.model.geo.GeoJSONPoint
import org.openremote.model.util.ValueUtil
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import javax.ws.rs.ForbiddenException
import javax.ws.rs.HttpMethod
import javax.ws.rs.client.ClientRequestContext
import javax.ws.rs.client.ClientRequestFilter
import javax.ws.rs.core.*

import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.security.ManagerIdentityProvider.OR_ADMIN_PASSWORD
import static org.openremote.manager.security.ManagerIdentityProvider.OR_ADMIN_PASSWORD_DEFAULT
import static org.openremote.model.Constants.*
import static org.openremote.model.value.ValueType.TEXT

class HttpServerProtocolTest extends Specification implements ManagerContainerTrait {

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
                        ObjectNode body = ValueUtil.parse(bodyStr).orElse(null)
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
                        ObjectNode body = ValueUtil.parse(bodyStr).orElse(null)
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

    def tryPost(TestResource authenticatedTestResource, Asset testAsset) {
        // There's some weird behaviour where sometimes a forbidden exception is thrown and sometimes not
        try {
            authenticatedTestResource.postAsset(testAsset)
        } catch (Exception ignored) {}
    }

    def "Check HTTP server protocol and JAX-RS deployment"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, initialDelay: 1)

        and: "the container starts"
        def container = startContainer(defaultConfig(), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def agentService = container.getService(AgentService.class)

        and: "an authenticated admin user"
        def accessToken = authenticate(
            container,
            MASTER_REALM,
            KEYCLOAK_CLIENT_ID,
            MASTER_REALM_ADMIN_USER,
            getString(container.getConfig(), OR_ADMIN_PASSWORD, OR_ADMIN_PASSWORD_DEFAULT)
        ).token

        and: "the test resource (to be deployed by the protocol)"
        def authenticatedTestResource = getClientTarget(
            serverUri(serverPort)
                    .path(AbstractHTTPServerProtocol.DEFAULT_DEPLOYMENT_PATH_PREFIX)
                    .path("test"),
            accessToken).proxy(TestResource.class)
        def testResource = getClientTarget(
                serverUri(serverPort)
                        .path(AbstractHTTPServerProtocol.DEFAULT_DEPLOYMENT_PATH_PREFIX)
                        .path("test"),
                null).proxy(TestResource.class)

        when: "a test HTTP server agent with a test deployment is created"
        def agent = new HTTPServerTestAgent("Test agent")
            .setRealm(MASTER_REALM)
            .setDeploymentPath("test")
            .setRoleBasedSecurity(true)

        and: "the agent is added to the asset service"
        agent = assetStorageService.merge(agent)

        then: "the protocol should be deployed"
        conditions.eventually {
            assert agentService.getProtocolInstance(agent.id) != null
            assert ((TestHTTPServerProtocol)agentService.getProtocolInstance(agent.id)).deployment != null
        }

        when: "the authenticated test resource is used to post an asset"
        def testAsset = new ThingAsset("Test Asset")
            .setId("12345")
            .addOrReplaceAttributes(
                new Attribute<>("attribute1", TEXT, "Test")
            )
            .setLocation(new GeoJSONPoint(1d, 2d))
        authenticatedTestResource.postAsset(testAsset)

        then: "the asset should be stored in the test protocol"
        conditions.eventually {
            def testServerProtocol = (TestHTTPServerProtocol)agentService.getProtocolInstance(agent.id)
            assert testServerProtocol != null
            assert testServerProtocol.resource1.postedAssets.size() == 1
            assert testServerProtocol.resource1.postedAssets.get(0).name == "Test Asset"
            assert testServerProtocol.resource1.postedAssets.get(0).type == ThingAsset.DESCRIPTOR.getName()
            assert testServerProtocol.resource1.postedAssets.get(0).getLocation().map{it.x}.orElse(null) == 1d
            assert testServerProtocol.resource1.postedAssets.get(0).getLocation().map{it.y}.orElse(null) == 2d
        }

        when: "the authenticated test resource is used to get an asset"
        def asset = authenticatedTestResource.getAsset("12345")

        then: "the asset should match the posted asset"
        assert asset.name == testAsset.name
        assert asset.type == testAsset.type
        assert asset.getLocation().map{it.coordinates.x}.orElse(-1d) == testAsset.getLocation().map{it.coordinates.x}.orElse(0d)
        assert asset.getLocation().map{it.coordinates.y}.orElse(-1d) == testAsset.getLocation().map{it.coordinates.y}.orElse(0d)

        when: "the un-authenticated test resource is used to post an asset"
        testResource.postAsset(testAsset)

        then: "an exception should be thrown"
        thrown ForbiddenException

        when: "an additional instance of the Test HTTP Server protocol is deployed without security enabled"
        def agent2 = new HTTPServerTestAgent("Test agent 2")
                .setRealm(MASTER_REALM)
                .setDeploymentPath("test2")
                .setRoleBasedSecurity(false)

        agent2 = assetStorageService.merge(agent2)

        and: "an un-authenticated test resource proxy is created for the new deployment"
        def testResource2 = getClientTarget(
                serverUri(serverPort)
                        .path(AbstractHTTPServerProtocol.DEFAULT_DEPLOYMENT_PATH_PREFIX)
                        .path("test2"),
                null).proxy(TestResource.class)

        then: "the protocol should be deployed"
        conditions.eventually {
            assert agentService.getProtocolInstance(agent2.id) != null
        }

        when: "the new test resource is used to post an asset"
        testAsset = new ThingAsset("Test Asset 2")
        testAsset.setId(UniqueIdentifierGenerator.generateId())
        testAsset.addOrReplaceAttributes(
            new Attribute<>("attribute2", TEXT, "Test")
        )
        testAsset.setLocation(new GeoJSONPoint(3d, 4d))
        testResource2.postAsset(testAsset)

        then: "the asset should be stored in the test protocol"
        conditions.eventually {
            def testServerProtocol = (TestHTTPServerProtocol)agentService.getProtocolInstance(agent2.id)
            assert testServerProtocol != null
            assert testServerProtocol.resource1.postedAssets.size() == 1
            assert testServerProtocol.resource1.postedAssets.get(0).name == "Test Asset 2"
            assert testServerProtocol.resource1.postedAssets.get(0).id == testAsset.id
            assert testServerProtocol.resource1.postedAssets.get(0).type == ThingAsset.DESCRIPTOR.getName()
            assert testServerProtocol.resource1.postedAssets.get(0).getLocation().map{it.x}.orElse(null) == 3d
            assert testServerProtocol.resource1.postedAssets.get(0).getLocation().map{it.y}.orElse(null) == 4d
        }

        when: "the new test resource is used to get an asset"
        asset = testResource2.getAsset(testAsset.id)

        then: "the asset should match the posted asset"
        assert asset.name == testAsset.name
        assert asset.type == testAsset.type
        assert asset.getLocation().map{it.coordinates.x}.orElse(-1d) == testAsset.getLocation().map{it.coordinates.x}.orElse(0d)
        assert asset.getLocation().map{it.coordinates.y}.orElse(-1d) == testAsset.getLocation().map{it.coordinates.y}.orElse(0d)

        when: "an agent is deleted"
        def protocolInstance = agentService.getProtocolInstance(agent.id) as TestHTTPServerProtocol
        def deploymentManager = Servlets.defaultContainer().getDeployment(protocolInstance.deployment.deploymentInfo.getDeploymentName());
        assetStorageService.delete([agent.id])

        then: "the associated protocol instance should be un-deployed"
        conditions.eventually {
            assert agentService.getProtocolInstance(agent.id) == null
            assert deploymentManager.getState() == DeploymentManager.State.UNDEPLOYED
        }

        then: "if an attempt is made to use the removed endpoint then no asset should be posted"
        conditions.eventually {
            def postCount = protocolInstance.resource1.postedAssets.size()
            tryPost(authenticatedTestResource, testAsset)
            assert postCount == protocolInstance.resource1.postedAssets.size()
        }
    }
}
