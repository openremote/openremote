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
import org.jboss.resteasy.plugins.interceptors.RoleBasedSecurityFeature;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.container.Container;
import org.openremote.container.json.JacksonConfig;
import org.openremote.container.json.ModelValueMessageBodyConverter;
import org.openremote.container.security.IdentityService;
import org.openremote.container.web.*;
import org.openremote.model.AbstractValueHolder;
import org.openremote.model.ValueHolder;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.agent.ProtocolConfiguration;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.ArrayValue;
import org.openremote.model.value.StringValue;
import org.openremote.model.value.Values;

import javax.servlet.ServletException;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Application;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static org.openremote.container.web.WebService.pathStartsWithHandler;
import static org.openremote.model.Constants.PROTOCOL_NAMESPACE;
import static org.openremote.model.Constants.REQUEST_HEADER_REALM;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

/**
 * This is an abstract protocol for creating JAX-RS deployments; a concrete implementation should be created for each
 * required JAX-RS deployment. Multiple instances of the same deployment can be created using multiple {@link
 * ProtocolConfiguration}s for the desired concrete implementation.
 * <p>
 * The deployment is deployed by creating a {@link ProtocolConfiguration} with the following {@link MetaItem}s:
 * <ul>
 * <li>{@link #META_PROTOCOL_DEPLOYMENT_PATH} <b>(required)</b></li>
 * <li>{@link #META_PROTOCOL_ALLOWED_ORIGINS}</li>
 * <li>{@link #META_PROTOCOL_ALLOWED_METHODS}</li>
 * </ul>
 * The path used for the deployment is determined by {@link #getDeploymentPath}. The realm that the {@link ProtocolConfiguration}s
 * {@link org.openremote.model.asset.Asset} belongs to will be used implicitly for all incoming requests to any deployments
 * that this protocol creates and therefore only users of that realm will be able to make calls to the deployment when
 * {@value META_PROTOCOL_ROLE_BASED_SECURITY_ENABLED} is true.
 */
public abstract class AbstractHttpServerProtocol extends AbstractProtocol {

    public static class DeploymentInstance {
        protected DeploymentInfo deploymentInfo;
        protected WebService.RequestHandler requestHandler;

        public DeploymentInstance(DeploymentInfo deploymentInfo, WebService.RequestHandler requestHandler) {
            this.deploymentInfo = deploymentInfo;
            this.requestHandler = requestHandler;
        }
    }

    public static final String PROTOCOL_NAME = PROTOCOL_NAMESPACE + ":httpServer";
    /**
     * This is the default path prefix for all deployments. Should not be overridden unless you know what you are doing
     * and there is a good reason to override.
     */
    public static final String DEFAULT_DEPLOYMENT_PATH_PREFIX = "/rest";
    /**
     * Sets the instance part of the path used for this deployment see {@link #getDeploymentPath}. Note the final full
     * path to a deployment must match the {@link #PATH_REGEX} filter.
     */
    public static final String META_PROTOCOL_DEPLOYMENT_PATH = PROTOCOL_NAME + ":deploymentPath";
    /**
     * Sets the allowed origins for CORS (incoming requests should include an origin header for CORS support). Note if
     * the container is started in development mode (i.e. {@link Container#DEV_MODE}=true) then all origins are allowed
     * to facilitate local development. Should be {@link StringValue} or an {@link ArrayValue} of {@link StringValue}s
     * containing the allowed origins.
     */
    public static final String META_PROTOCOL_ALLOWED_ORIGINS = PROTOCOL_NAME + ":allowedOrigins";
    /**
     * Sets the allowed methods for CORS (incoming requests should include an origin header for CORS support). Should be
     * a comma separated string of allowed {@link HttpMethod}s (e.g. "OPTIONS, GET, POST") (default:
     * {@value #DEFAULT_ALLOWED_METHODS}).
     */
    public static final String META_PROTOCOL_ALLOWED_METHODS = PROTOCOL_NAME + ":allowedMethods";
    /**
     * Flag to enable role based security using the {@link RoleBasedSecurityFeature}; also requires that the
     * {@link Container} is started with an {@link IdentityService}.
     */
    public static final String META_PROTOCOL_ROLE_BASED_SECURITY_ENABLED = PROTOCOL_NAME + ":roleBasedSecurity";
    /**
     * The regex used to validate the deployment path.
     */
    public static final Pattern PATH_REGEX = Pattern.compile("^[\\w/_]+$", Pattern.CASE_INSENSITIVE);
    public static final String DEFAULT_ALLOWED_METHODS = "OPTIONS, GET, POST, DELETE, PUT, PATCH";
    public static final String DEFAULT_DEPLOYMENT_NAME_FORMAT = "HttpServer %1$s Deployment %2$d";
    protected static final Map<AttributeRef, DeploymentInstance> deployments = new HashMap<>();
    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, AbstractHttpServerProtocol.class);
    protected static WebServiceExceptions.DefaultResteasyExceptionMapper defaultResteasyExceptionMapper;
    protected static WebServiceExceptions.ForbiddenResteasyExceptionMapper forbiddenResteasyExceptionMapper;
    protected static JacksonConfig jacksonConfig;
    protected static ModelValueMessageBodyConverter modelValueMessageBodyConverter;
    protected static AlreadyGzippedWriterInterceptor alreadtGzippedWriterInterceptor;
    protected static ClientErrorExceptionHandler clientErrorExceptionHandler;
    protected static WebServiceExceptions.ServletUndertowExceptionHandler undertowExceptionHandler;
    protected int deploymentCounter = 0;
    protected Container container;
    protected boolean devMode;
    protected IdentityService identityService;
    protected WebService webService;

    @Override
    public void init(Container container) throws Exception {
        super.init(container);
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
            modelValueMessageBodyConverter = new ModelValueMessageBodyConverter();
            alreadtGzippedWriterInterceptor = new AlreadyGzippedWriterInterceptor();
            clientErrorExceptionHandler = new ClientErrorExceptionHandler();
        }
    }

    @Override
    protected void doLinkProtocolConfiguration(Asset agent, AssetAttribute protocolConfiguration) {
        Application application = createApplication(protocolConfiguration);
        ResteasyDeployment deployment = createDeployment(application, protocolConfiguration);
        DeploymentInfo deploymentInfo = createDeploymentInfo(deployment, protocolConfiguration);
        configureDeploymentInfo(deploymentInfo);
        deploy(deploymentInfo, protocolConfiguration);
    }

    @Override
    protected void doUnlinkProtocolConfiguration(Asset agent, AssetAttribute protocolConfiguration) {
        undeploy(protocolConfiguration);
    }

    protected Application createApplication(AssetAttribute protocolConfiguration) {
        List<Object> providers = getStandardProviders(protocolConfiguration);
        providers = providers == null ? new ArrayList<>() : providers;
        providers.addAll(getApiSingletons(protocolConfiguration));
        return new WebApplication(container, null, providers);
    }

    protected ResteasyDeployment createDeployment(Application application, AssetAttribute protocolConfiguration) {
        ResteasyDeployment resteasyDeployment = new ResteasyDeployment();
        resteasyDeployment.setApplication(application);

        List<String> allowedOrigins;

        if (devMode) {
            allowedOrigins = Collections.singletonList("*");
        } else {
            Optional<MetaItem> allowedOriginsMeta = protocolConfiguration
                    .getMetaItem(META_PROTOCOL_ALLOWED_ORIGINS);

            allowedOrigins = allowedOriginsMeta
                .flatMap(AbstractValueHolder::getValueAsString)
                .map(originString -> Arrays.asList(originString.split(";")))
                    .orElseGet(() ->
                            allowedOriginsMeta.flatMap(AbstractValueHolder::getValueAsArray)
                                    .flatMap(arrayValue ->
                                            Values.getArrayElements(
                                                    arrayValue,
                                                    StringValue.class,
                                                    true,
                                                    false,
                                                    StringValue::getString))
                                    .orElse(null));
        }

        if (allowedOrigins != null) {
            String allowedMethods = protocolConfiguration
                    .getMetaItem(META_PROTOCOL_ALLOWED_METHODS)
                    .flatMap(AbstractValueHolder::getValueAsString)
                    .orElse(DEFAULT_ALLOWED_METHODS);

            if (TextUtil.isNullOrEmpty(allowedMethods)) {
                throw new IllegalArgumentException("Allowed methods meta item must be a non empty string: "
                        + META_PROTOCOL_ALLOWED_METHODS);
            }

            CorsFilter corsFilter = new CorsFilter();
            corsFilter.getAllowedOrigins().addAll(allowedOrigins);
            corsFilter.setAllowedMethods(allowedMethods);
            resteasyDeployment.getProviders().add(corsFilter);
        }

        return resteasyDeployment;
    }

    protected DeploymentInfo createDeploymentInfo(ResteasyDeployment resteasyDeployment, AssetAttribute protocolConfiguration) {
        String deploymentPath = getDeploymentPath(protocolConfiguration);
        String deploymentName = getDeploymentName(protocolConfiguration);

        boolean enableSecurity = protocolConfiguration.getMetaItem(META_PROTOCOL_ROLE_BASED_SECURITY_ENABLED)
                .flatMap(AbstractValueHolder::getValueAsBoolean)
                .orElse(false);

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
    abstract protected Set<Object> getApiSingletons(AssetAttribute protocolConfiguration);

    /**
     * Get the path prefix to use for this protocol instance; should use {@value #DEFAULT_DEPLOYMENT_PATH_PREFIX} unless there
     * is a good reason to override this.
     */
    protected String getDeploymentPathPrefix() {
        return DEFAULT_DEPLOYMENT_PATH_PREFIX;
    }

    /**
     * Deployment path will always be prefixed with {@link #getDeploymentPathPrefix()}; default implementation combines
     * the prefix with the value of {@link #META_PROTOCOL_DEPLOYMENT_PATH}, for example:
     * <ul>
     * <li>getDeploymentPathPrefix() = {@value #DEFAULT_DEPLOYMENT_PATH_PREFIX}</li>
     * <li>{@link #META_PROTOCOL_DEPLOYMENT_PATH} = "complaints"</li>
     * </ul>
     * <p>
     * Full path to deployment = "/rest/complaints"
     * <p>
     * If the {@link #META_PROTOCOL_DEPLOYMENT_PATH} is missing or not a {@link StringValue} or the generated path does
     * not match the {@link #PATH_REGEX} regex then an {@link IllegalArgumentException} will is thrown.
     */
    protected String getDeploymentPath(AssetAttribute protocolConfiguration) throws IllegalArgumentException {
        String path = protocolConfiguration.getMetaItem(META_PROTOCOL_DEPLOYMENT_PATH)
                .flatMap(ValueHolder::getValueAsString)
                .map(String::toLowerCase)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Required deployment path meta item is missing or invalid: " + META_PROTOCOL_DEPLOYMENT_PATH));

        String deploymentPath = getDeploymentPathPrefix() + "/" + path;

        if (!PATH_REGEX.matcher(deploymentPath).find()) {
            throw new IllegalArgumentException(
                    "Required deployment path meta item is missing or invalid: " + META_PROTOCOL_DEPLOYMENT_PATH);
        }

        return deploymentPath;
    }

    /**
     * Get standard JAX-RS providers that are used in all deployments.
     */
    protected List<Object> getStandardProviders(AssetAttribute protocolConfiguration) {
        return Lists.newArrayList(
                defaultResteasyExceptionMapper,
                forbiddenResteasyExceptionMapper,
                jacksonConfig,
                modelValueMessageBodyConverter,
                alreadtGzippedWriterInterceptor,
                clientErrorExceptionHandler
        );
    }

    protected void configureDeploymentInfo(DeploymentInfo deploymentInfo) {
        // This will catch anything not handled by Resteasy/Servlets, such as IOExceptions "at the wrong time"
        deploymentInfo.setExceptionHandler(undertowExceptionHandler);
    }

    /**
     * Get a unique deployment name for the supplied {@link ProtocolConfiguration}.
     */
    protected String getDeploymentName(AssetAttribute protocolConfiguration) {
        deploymentCounter++;
        return String.format(DEFAULT_DEPLOYMENT_NAME_FORMAT, getProtocolDisplayName(), deploymentCounter);
    }

    protected void deploy(DeploymentInfo deploymentInfo, AssetAttribute protocolConfiguration) {
        LOG.info("Deploying JAX-RS deployment for: " + protocolConfiguration.getReferenceOrThrow());
        DeploymentManager manager = Servlets.defaultContainer().addDeployment(deploymentInfo);
        manager.deploy();
        HttpHandler httpHandler;

        // Get realm from owning agent asset
        Asset agent = assetService.getAgent(protocolConfiguration);
        String agentRealm = agent.getRealm();

        if (TextUtil.isNullOrEmpty(agentRealm)) {
            throw new IllegalStateException("Cannot determine the realm that this protocol configuration belongs to");
        }

        try {
            httpHandler = manager.start();

            // Wrap the handler to inject the realm
            HttpHandler handlerWrapper = exchange -> {
                exchange.getRequestHeaders().put(HttpString.tryFromString(REQUEST_HEADER_REALM), agentRealm);
                httpHandler.handleRequest(exchange);
            };
            WebService.RequestHandler requestHandler = pathStartsWithHandler(deploymentInfo.getDeploymentName(), deploymentInfo.getContextPath(), handlerWrapper);
            DeploymentInstance deploymentInstance = new DeploymentInstance(deploymentInfo, requestHandler);
            deployments.put(protocolConfiguration.getReferenceOrThrow(), deploymentInstance);

            LOG.info("Registering HTTP Server Protocol request handler '"
                    + this.getClass().getSimpleName()
                    + "' for request path: "
                    + deploymentInfo.getContextPath());
            // Add the handler before the greedy deployment handler
            webService.getRequestHandlers().add(0, requestHandler);
        } catch (ServletException e) {
            LOG.severe("Failed to deploy deployment: " + deploymentInfo.getDeploymentName());
        }
    }

    protected void undeploy(AssetAttribute protocolConfiguration) {

        DeploymentInstance instance = deployments.get(protocolConfiguration.getReferenceOrThrow());

        if (instance == null) {
            LOG.info("Deployment doesn't exist for protocol configuration: " + protocolConfiguration);
            return;
        }
        try {
            LOG.info("Un-registering HTTP Server Protocol request handler '"
                    + this.getClass().getSimpleName()
                    + "' for request path: "
                    + instance.deploymentInfo.getContextPath());
            webService.getRequestHandlers().remove(instance.requestHandler);
            DeploymentManager manager = Servlets.defaultContainer().getDeployment(instance.deploymentInfo.getDeploymentName());
            manager.stop();
            manager.undeploy();
            Servlets.defaultContainer().removeDeployment(instance.deploymentInfo);
            deployments.remove(protocolConfiguration.getReferenceOrThrow());
        } catch (Exception ex) {
            LOG.log(Level.WARNING,
                    "An exception occurred whilst un-deploying protocolConfiguration: " + protocolConfiguration.getReferenceOrThrow(),
                    ex);
            throw new RuntimeException(ex);
        }

    }
}
