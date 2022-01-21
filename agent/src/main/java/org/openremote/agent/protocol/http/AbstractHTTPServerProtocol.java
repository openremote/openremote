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

import com.google.common.collect.Lists;
import io.undertow.server.HttpHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.util.HttpString;
import org.jboss.resteasy.plugins.interceptors.CorsFilter;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.container.json.JacksonConfig;
import org.openremote.container.security.IdentityService;
import org.openremote.container.web.*;
import org.openremote.model.Constants;
import org.openremote.model.Container;
import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TextUtil;

import javax.servlet.ServletException;
import javax.ws.rs.core.Application;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.openremote.container.web.WebService.pathStartsWithHandler;
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

    public static class DeploymentInstance {
        protected DeploymentInfo deploymentInfo;
        protected WebService.RequestHandler requestHandler;

        public DeploymentInstance(DeploymentInfo deploymentInfo, WebService.RequestHandler requestHandler) {
            this.deploymentInfo = deploymentInfo;
            this.requestHandler = requestHandler;
        }
    }

    /**
     * This is the default path prefix for all deployments. Should not be overridden unless you know what you are doing
     * and there is a good reason to override.
     */
    public static final String DEFAULT_DEPLOYMENT_PATH_PREFIX = "/rest";

    /**
     * The regex used to validate the deployment path.
     */
    public static final Pattern PATH_REGEX = Pattern.compile("^[\\w/_]+$", Pattern.CASE_INSENSITIVE);
    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, AbstractHTTPServerProtocol.class);
    public static final HTTPMethod[] DEFAULT_ALLOWED_METHODS = HTTPMethod.values();
    protected DeploymentInstance deployment;
    protected static WebServiceExceptions.DefaultResteasyExceptionMapper defaultResteasyExceptionMapper;
    protected static WebServiceExceptions.ForbiddenResteasyExceptionMapper forbiddenResteasyExceptionMapper;
    protected static JacksonConfig jacksonConfig;
    protected static AlreadyGzippedWriterInterceptor alreadyGzippedWriterInterceptor;
    protected static ClientErrorExceptionHandler clientErrorExceptionHandler;
    protected static WebServiceExceptions.ServletUndertowExceptionHandler undertowExceptionHandler;
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

        if (defaultResteasyExceptionMapper == null) {
            defaultResteasyExceptionMapper = new WebServiceExceptions.DefaultResteasyExceptionMapper(devMode);
            forbiddenResteasyExceptionMapper = new WebServiceExceptions.ForbiddenResteasyExceptionMapper(devMode);
            undertowExceptionHandler = new WebServiceExceptions.ServletUndertowExceptionHandler(devMode);
            jacksonConfig = new JacksonConfig();
            alreadyGzippedWriterInterceptor = new AlreadyGzippedWriterInterceptor();
            clientErrorExceptionHandler = new ClientErrorExceptionHandler();
        }

        Application application = createApplication();
        ResteasyDeployment deployment = createDeployment(application);
        DeploymentInfo deploymentInfo = createDeploymentInfo(deployment);
        configureDeploymentInfo(deploymentInfo);
        deploy(deploymentInfo);
    }

    @Override
    protected void doStop(Container container) throws Exception {
        undeploy();
    }

    protected Application createApplication() {
        List<Object> providers = getStandardProviders();
        providers = providers == null ? new ArrayList<>() : providers;
        providers.addAll(getApiSingletons());
        return new WebApplication(container, null, providers);
    }

    protected ResteasyDeployment createDeployment(Application application) {
        ResteasyDeployment resteasyDeployment = new ResteasyDeployment();
        resteasyDeployment.setApplication(application);

        List<String> allowedOrigins;

        if (devMode) {
            allowedOrigins = Collections.singletonList("*");
        } else {
            allowedOrigins = agent.getAllowedOrigins().map(Arrays::asList).orElse(null);
        }

        if (allowedOrigins != null) {
            String allowedMethods = Arrays.stream(agent.getAllowedHTTPMethods().orElse(DEFAULT_ALLOWED_METHODS)).map(Enum::name).collect(Collectors.joining(","));
            CorsFilter corsFilter = new CorsFilter();
            corsFilter.getAllowedOrigins().addAll(allowedOrigins);
            corsFilter.setAllowedMethods(allowedMethods);
            resteasyDeployment.getProviders().add(corsFilter);
        }

        return resteasyDeployment;
    }

    protected DeploymentInfo createDeploymentInfo(ResteasyDeployment resteasyDeployment) {
        String deploymentPath = getDeploymentPath();
        String deploymentName = getDeploymentName();

        boolean enableSecurity = agent.isRoleBasedSecurity().orElse(false);

        if (enableSecurity) {
            if (identityService == null) {
                throw new RuntimeException("Role based security can only be enabled when an identity service is available");
            }
        }

        resteasyDeployment.setSecurityEnabled(enableSecurity);

        ServletInfo resteasyServlet = Servlets.servlet("ResteasyServlet", HttpServlet30Dispatcher.class)
                .setAsyncSupported(true)
                .setLoadOnStartup(1)
                .addMapping("/*");

        DeploymentInfo deploymentInfo = new DeploymentInfo()
                .setDeploymentName(deploymentName)
                .setContextPath(deploymentPath)
                .addServletContextAttribute(ResteasyDeployment.class.getName(), resteasyDeployment)
                .addServlet(resteasyServlet)
                .setClassLoader(Container.class.getClassLoader());

        if (enableSecurity) {
            identityService.secureDeployment(deploymentInfo);
        }

        return deploymentInfo;
    }

    /**
     * Should return instances of all JAX-RS interface implementations that make up this protocol's deployment.
     */
    abstract protected Set<Object> getApiSingletons();

    /**
     * Get the path prefix to use for this protocol instance; should use {@value #DEFAULT_DEPLOYMENT_PATH_PREFIX} unless there
     * is a good reason to override this.
     */
    protected String getDeploymentPathPrefix() {
        return DEFAULT_DEPLOYMENT_PATH_PREFIX;
    }

    /**
     * Deployment path will always be prefixed with {@link #getDeploymentPathPrefix()}; default implementation combines
     * the prefix with the value of {@link AbstractHTTPServerAgent# META_PROTOCOL_DEPLOYMENT_PATH}, for example:
     * <ul>
     * <li>getDeploymentPathPrefix() = {@value #DEFAULT_DEPLOYMENT_PATH_PREFIX}</li>
     * <li>{@link AbstractHTTPServerAgent#DEPLOYMENT_PATH} = "complaints"</li>
     * </ul>
     * <p>
     * Full path to deployment = "/rest/complaints"
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

        String deploymentPath = getDeploymentPathPrefix() + "/" + path;

        if (!PATH_REGEX.matcher(deploymentPath).find()) {
            throw new IllegalArgumentException(
                    "Required deployment path attribute is missing or invalid: " + agent);
        }

        return deploymentPath;
    }

    /**
     * Get standard JAX-RS providers that are used in the deployment.
     */
    protected List<Object> getStandardProviders() {
        return Lists.newArrayList(
            defaultResteasyExceptionMapper,
            forbiddenResteasyExceptionMapper,
            jacksonConfig,
            alreadyGzippedWriterInterceptor,
            clientErrorExceptionHandler
        );
    }

    protected void configureDeploymentInfo(DeploymentInfo deploymentInfo) {
        // This will catch anything not handled by Resteasy/Servlets, such as IOExceptions "at the wrong time"
        deploymentInfo.setExceptionHandler(undertowExceptionHandler);
    }

    /**
     * Get a unique deployment name for this instance.
     */
    protected String getDeploymentName() {
        return "HttpServerProtocol=" + getClass().getSimpleName() + ",  Agent ID=" + agent.getId();
    }

    protected void deploy(DeploymentInfo deploymentInfo) {
        LOG.info("Deploying JAX-RS deployment for protocol instance : " + this);
        DeploymentManager manager = Servlets.defaultContainer().addDeployment(deploymentInfo);
        manager.deploy();
        HttpHandler httpHandler;

        // Get realm from owning agent asset
        String agentRealm = agent.getRealm();

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
            webService.getRequestHandlers().add(0, requestHandler);

            deployment = new DeploymentInstance(deploymentInfo, requestHandler);
        } catch (ServletException e) {
            LOG.severe("Failed to deploy deployment: " + deploymentInfo.getDeploymentName());
        }
    }

    protected void undeploy() {

        if (deployment == null) {
            LOG.info("Deployment doesn't exist for protocol instance: " + this);
            return;
        }

        try {
            LOG.info("Un-registering HTTP Server Protocol request handler '"
                    + this.getClass().getSimpleName()
                    + "' for request path: "
                    + deployment.deploymentInfo.getContextPath());
            webService.getRequestHandlers().remove(deployment.requestHandler);
            DeploymentManager manager = Servlets.defaultContainer().getDeployment(deployment.deploymentInfo.getDeploymentName());
            if (manager != null) {
                manager.stop();
                manager.undeploy();
            }
            Servlets.defaultContainer().removeDeployment(deployment.deploymentInfo);
        } catch (Exception ex) {
            LOG.log(Level.WARNING,
                    "An exception occurred whilst un-deploying protocol instance: " + this,
                    ex);
            throw new RuntimeException(ex);
        }
    }

    @Override
    public String getProtocolInstanceUri() {
        return "httpServer://" + getDeploymentPath();
    }
}
