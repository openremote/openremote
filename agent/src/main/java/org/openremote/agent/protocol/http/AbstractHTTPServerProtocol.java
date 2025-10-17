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
package org.openremote.agent.protocol.http;

import io.undertow.server.HttpHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.util.HttpString;
import jakarta.servlet.ServletException;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.container.security.IdentityService;
import org.openremote.container.web.*;
import org.openremote.model.Constants;
import org.openremote.model.Container;
import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.syslog.SyslogCategory;

import jakarta.ws.rs.core.Application;
import org.openremote.model.util.TextUtil;

import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.System.Logger.Level.INFO;
import static org.openremote.container.web.WebService.*;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

/**
 * This is an abstract protocol for creating JAX-RS deployments; a concrete implementation should be created for each
 * required JAX-RS deployment. Multiple instances of the same deployment can be created by creating multiple protocol
 * instances for the desired concrete implementation.
 * <p>
 * The deployment is deployed by creating an instance of a concrete {@link AbstractHTTPServerAgent} implementation with
 * the following {@link Attribute}s:
 * <ul>
 * <li>{@link AbstractHTTPServerAgent#DEPLOYMENT_PATH} <b>(required)</b></li>
 * <li>{@link AbstractHTTPServerAgent#ALLOWED_ORIGINS}</li>
 * <li>{@link AbstractHTTPServerAgent#ALLOWED_HTTP_METHODS}</li>
 * </ul>
 * The path used for the deployment is determined by {@link #getDeploymentPath}. The realm that the {@link Agent}
 * belongs to will be used implicitly for all incoming requests to any deployments
 * that this protocol creates and therefore only users of that realm will be able to make calls to the deployment when
 * {@link AbstractHTTPServerAgent#ROLE_BASED_SECURITY} is true.
 */
public abstract class AbstractHTTPServerProtocol<T extends AbstractHTTPServerProtocol<T, U, V>, U extends AbstractHTTPServerAgent<U, T, V>, V extends AgentLink<?>> extends AbstractProtocol<U, V> {

    /**
     * This is the path prefix for all HTTP server agent deployments
     */
    public static final String DEPLOYMENT_PATH_PREFIX = "/http_agent";

    /**
     * The regex used to validate the deployment path.
     */
    public static final Pattern PATH_REGEX = Pattern.compile("^[\\w/_]+$", Pattern.CASE_INSENSITIVE);
    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, AbstractHTTPServerProtocol.class);
    protected WebService.DeploymentInstance deployment;
    protected Container container;
    protected boolean devMode;
    protected IdentityService identityService;
    protected WebService webService;

    public AbstractHTTPServerProtocol(U agent) {
        super(agent);
    }

    @Override
    public void doStart(Container container) throws Exception {
        this.container = container;
        this.devMode = container.isDevMode();

        identityService = container.hasService(IdentityService.class)
                ? container.getService(IdentityService.class)
                : null;

        webService = container.getService(WebService.class);

        boolean secure = agent.isRoleBasedSecurity().orElse(false);
        String deploymentPath = getDeploymentPath();
        String deploymentName = getDeploymentName();

       Application application = new WebApplication(
          container,
          null,
          Stream.of(
             devMode ? getStandardProviders(devMode, 1) : getStandardProviders(devMode, 1,
                agent.getAllowedOrigins().map(Set::of).orElse(null),
                agent.getAllowedHTTPMethods().map(methods ->
                        Arrays.stream(methods).map(Enum::name)
                                .collect(Collectors.joining(","))).orElse(DEFAULT_CORS_ALLOW_ALL),
                DEFAULT_CORS_ALLOW_ALL,
                DEFAULT_CORS_MAX_AGE,
                DEFAULT_CORS_ALLOW_CREDENTIALS),
             getApiSingletons()).flatMap(Collection::stream).toList());

        ResteasyDeployment deployment = createResteasyDeployment(application, identityService, secure);
        DeploymentInfo deploymentInfo = createDeploymentInfo(deployment, deploymentPath, deploymentName);
        deploy(deploymentInfo, agent.getRealm());
    }

    @Override
    protected void doStop(Container container) throws Exception {
        undeploy();
    }

    /**
     * Should return instances of all JAX-RS interface implementations that make up this protocol's deployment.
     */
    abstract protected Set<Object> getApiSingletons();

    /**
     * Deployment path will always be prefixed with {@link #DEPLOYMENT_PATH_PREFIX}; and {@link Agent#getRealm()} then
     * combines the prefix with the value of {@link AbstractHTTPServerAgent#DEPLOYMENT_PATH}, for example an agent in
     * a realm called manufacturer:
     * <ul>
     * <li>{@link AbstractHTTPServerAgent#DEPLOYMENT_PATH} = "complaints"</li>
     * </ul>
     * <p>
     * Full path to deployment = "{@value #DEPLOYMENT_PATH_PREFIX}/manufacturer/complaints"
     * <p>
     * If the {@link AbstractHTTPServerAgent#DEPLOYMENT_PATH} is missing or not a String or the generated path does
     * not match the {@link #PATH_REGEX} regex then an {@link IllegalArgumentException} will is thrown.
     */
    protected String getDeploymentPath() throws IllegalArgumentException {
        String path = agent.getDeploymentPath()
                .map(String::toLowerCase)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Required deployment path attribute is missing or invalid: " + agent));

        String deploymentPath = DEPLOYMENT_PATH_PREFIX + "/" + agent.getRealm() + "/" + path;

        if (!PATH_REGEX.matcher(deploymentPath).find()) {
            throw new IllegalArgumentException(
                    "Required deployment path attribute is missing or invalid: " + agent);
        }

        return deploymentPath;
    }

    /**
     * Get a unique deployment name for this instance.
     */
    protected String getDeploymentName() {
        return "HttpServerProtocol=" + getClass().getSimpleName() + ", Agent ID=" + agent.getId();
    }

    public static WebService.RequestHandler deploy(DeploymentInfo deploymentInfo, String agentRealm) {
        LOG.log(INFO, "Deploying JAX-RS deployment for protocol instance : " + this);
        DeploymentManager manager = Servlets.defaultContainer().addDeployment(deploymentInfo);
        manager.deploy();
        HttpHandler httpHandler;

        if (TextUtil.isNullOrEmpty(agentRealm)) {
            throw new IllegalStateException("Cannot determine the realm that this agent belongs to");
        }

        try {
            httpHandler = manager.start();

            // Wrap the handler to inject the realm
            HttpHandler handlerWrapper = exchange -> {
                exchange.getRequestHeaders().put(HttpString.tryFromString(Constants.REALM_PARAM_NAME), agentRealm);
                httpHandler.handleRequest(exchange);
            };
            WebService.RequestHandler requestHandler = pathStartsWithHandler(deploymentInfo.getDeploymentName(), deploymentInfo.getContextPath(), handlerWrapper);

            LOG.info("Registering HTTP Server Protocol request handler '"
                    + this.getClass().getSimpleName()
                    + "' for request path: "
                    + deploymentInfo.getContextPath());
            // Add the handler before the greedy deployment handler
            webService.getRequestHandlers().addFirst(requestHandler);

            deployment = new WebService.DeploymentInstance(deploymentInfo, requestHandler);
        } catch (ServletException e) {
            LOG.severe("Failed to deploy deployment: " + deploymentInfo.getDeploymentName());
        }
    }

    @Override
    public String getProtocolInstanceUri() {
        return "httpServer://" + getDeploymentPath();
    }
}
