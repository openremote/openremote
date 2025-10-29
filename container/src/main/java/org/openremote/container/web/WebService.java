/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.container.web;

import com.google.common.collect.Lists;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.CanonicalPathHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.RequestDumpingHandler;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.handlers.DefaultServlet;
import io.undertow.servlet.util.ImmediateInstanceHandle;
import io.undertow.util.HttpString;
import io.undertow.websockets.core.WebSocketChannel;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.UriBuilder;
import org.jboss.resteasy.core.ResteasyDeploymentImpl;
import org.jboss.resteasy.plugins.interceptors.AcceptEncodingGZIPFilter;
import org.jboss.resteasy.plugins.interceptors.CorsFilter;
import org.jboss.resteasy.plugins.interceptors.GZIPDecodingInterceptor;
import org.jboss.resteasy.plugins.interceptors.GZIPEncodingInterceptor;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.openremote.container.json.JacksonConfig;
import org.openremote.container.security.IdentityService;
import org.openremote.container.web.file.GzipResponseFilter;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.util.TextUtil;
import org.xnio.Options;

import java.net.Inet4Address;
import java.net.URI;
import java.util.*;

import static java.lang.System.Logger.Level.*;
import static org.openremote.container.util.MapAccess.*;
import static org.openremote.container.web.file.FileServlet.MIME_TYPES_TO_ZIP;
import static org.openremote.model.Constants.*;

public abstract class WebService implements ContainerService {

    // Change this to 0.0.0.0 to bind on all interfaces, enabling
    // access of the manager service from other devices in your LAN
    public static final String OR_WEBSERVER_LISTEN_HOST = "OR_WEBSERVER_LISTEN_HOST";
    public static final String OR_WEBSERVER_LISTEN_HOST_DEFAULT = "0.0.0.0";
    public static final String OR_WEBSERVER_LISTEN_PORT = "OR_WEBSERVER_LISTEN_PORT";
    public static final int OR_WEBSERVER_LISTEN_PORT_DEFAULT = 8080;
    public static final String OR_WEBSERVER_DUMP_REQUESTS = "OR_WEBSERVER_DUMP_REQUESTS";
    public static final boolean OR_WEBSERVER_DUMP_REQUESTS_DEFAULT = false;
    public static final String OR_WEBSERVER_ALLOWED_ORIGINS = "OR_WEBSERVER_ALLOWED_ORIGINS";
    public static final String OR_WEBSERVER_ALLOWED_METHODS = "OR_WEBSERVER_ALLOWED_METHODS";
    public static final String OR_WEBSERVER_EXPOSED_HEADERS = "OR_WEBSERVER_EXPOSED_HEADERS";
    public static final String OR_WEBSERVER_IO_THREADS_MAX = "OR_WEBSERVER_IO_THREADS_MAX";
    public static final int OR_WEBSERVER_IO_THREADS_MAX_DEFAULT = Math.max(Runtime.getRuntime().availableProcessors(), 2);
    public static final String OR_WEBSERVER_WORKER_THREADS_MAX = "OR_WEBSERVER_WORKER_THREADS_MAX";
    public static final int OR_WEBSERVER_WORKER_THREADS_MAX_DEFAULT = Math.max(Runtime.getRuntime().availableProcessors(), 10);
    private static final System.Logger LOG = System.getLogger(WebService.class.getName());
    public static final int DEFAULT_CORS_MAX_AGE = 1209600;
    public static final String DEFAULT_CORS_ALLOW_ALL = "*";
    public static final boolean DEFAULT_CORS_ALLOW_CREDENTIALS = true;
    protected boolean devMode;
    protected String host;
    protected IdentityService identityService;
    protected int port;
    protected Undertow undertow;
    protected URI containerHostUri;
    protected PathHandler pathHandler = Handlers.path();

    protected static String getLocalIpAddress() throws Exception {
        return Inet4Address.getLocalHost().getHostAddress();
    }

    @Override
    public int getPriority() {
        return LOW_PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {
       identityService = container.getService(IdentityService.class);
        devMode = container.isDevMode();
        host = getString(container.getConfig(), OR_WEBSERVER_LISTEN_HOST, OR_WEBSERVER_LISTEN_HOST_DEFAULT);
        port = getInteger(container.getConfig(), OR_WEBSERVER_LISTEN_PORT, OR_WEBSERVER_LISTEN_PORT_DEFAULT);
        String containerHost = host.equalsIgnoreCase("localhost") || host.indexOf("127") == 0 || host.indexOf("0.0.0.0") == 0
                ? getLocalIpAddress()
                : host;

        containerHostUri =
                UriBuilder.fromPath("/")
                        .scheme("http")
                        .host(containerHost)
                        .port(port).build();

        undertow = build(
                container,
                Undertow.builder()
                        .addHttpListener(port, host)
                        .setIoThreads(getInteger(container.getConfig(), OR_WEBSERVER_IO_THREADS_MAX, OR_WEBSERVER_IO_THREADS_MAX_DEFAULT))
                        .setWorkerThreads(getInteger(container.getConfig(), OR_WEBSERVER_WORKER_THREADS_MAX, OR_WEBSERVER_WORKER_THREADS_MAX_DEFAULT))
                        .setWorkerOption(Options.WORKER_NAME, "WebService")
                        .setWorkerOption(Options.THREAD_DAEMON, true)
        ).build();

        // We have to set system properties for websocket timeouts
        System.setProperty(WebSocketChannel.WEB_SOCKETS_READ_TIMEOUT, "30000");
        System.setProperty(WebSocketChannel.WEB_SOCKETS_WRITE_TIMEOUT, "30000");
    }

    @Override
    public void start(Container container) throws Exception {
        if (undertow != null) {
            undertow.start();
            LOG.log(INFO, "Webserver ready on http://" + host + ":" + port);
        }
    }

    @Override
    public void stop(Container container) throws Exception {
        if (undertow != null) {
            undertow.stop();
            undertow = null;
        }
    }

   /**
    * Deploys a servlet undertow deployment.
    */
    public void deploy(DeploymentInfo deploymentInfo, boolean useCanonicalPathHandler) {
       String pathPrefix = deploymentInfo.getContextPath();

       if (!pathPrefix.startsWith("/")) {
          pathPrefix = "/" + pathPrefix;
       }

        LOG.log(INFO, "Deploying undertow servlet deployment: name=" + deploymentInfo.getDeploymentName() + ", path=" + pathPrefix + ", secure=" + !deploymentInfo.isSecurityDisabled());

       try {
           DeploymentManager manager = Servlets.defaultContainer().addDeployment(deploymentInfo);
           manager.deploy();
           HttpHandler httpHandler = manager.start();

           if (useCanonicalPathHandler) {
              LOG.log(DEBUG, "Using canonical path handler for deployment");
              httpHandler = new CanonicalPathHandler(httpHandler);
           }

           pathHandler.addPrefixPath(deploymentInfo.getContextPath(), httpHandler);
        } catch (ServletException e) {
            LOG.log(ERROR, "Servlet deployment failed: " + e.getMessage());
        }
    }

    public void deployHttpHandler(String pathPrefix, HttpHandler httpHandler) {
       if (!pathPrefix.startsWith("/")) {
          pathPrefix = "/" + pathPrefix;
       }

        LOG.log(INFO, "Deploying undertow http handler: path=" + pathPrefix);
        pathHandler.addPrefixPath(pathPrefix, httpHandler);
    }

    public void undeploy(String deploymentName) {
       try {
          DeploymentManager manager = Servlets.defaultContainer().getDeployment(deploymentName);
          DeploymentInfo deploymentInfo = manager.getDeployment().getDeploymentInfo();
          LOG.log(INFO, "Un-deploying undertow servlet deployment: name=" + deploymentInfo.getDeploymentName() + ", path=" + deploymentInfo.getContextPath());
          pathHandler.removePrefixPath(deploymentInfo.getContextPath());
          manager.stop();
          manager.undeploy();
          Servlets.defaultContainer().removeDeployment(deploymentInfo);
       } catch (Exception ex) {
           LOG.log(ERROR, "Servlet un-deployment failed: name=" + deploymentName + ", exception=" + ex.getMessage());
       }
    }

    public void undeployHttpHandler(String pathPrefix) {
       LOG.log(INFO, "Un-deploying undertow handler: path=" + pathPrefix);
       pathHandler.removePrefixPath(pathPrefix);
    }

    public void addPathPrefixHandler(String path, HttpHandler handler) {
        pathHandler.addPrefixPath(path, handler);
    }

    public void removePathPrefixHandler(String path) {
        pathHandler.removePrefixPath(path);
    }

    /**
     * Get standard JAX-RS providers that are used in the deployment with optional realm extraction from the request
     * path and default CORS behaviour
     */
    public static List<Object> getStandardProviders(boolean devMode, Integer realmIndex) {
        return getStandardProviders(
            devMode,
            realmIndex,
            devMode ? Collections.singleton(DEFAULT_CORS_ALLOW_ALL) : null,
            devMode ? DEFAULT_CORS_ALLOW_ALL : null,
            devMode ? DEFAULT_CORS_ALLOW_ALL : null,
            DEFAULT_CORS_MAX_AGE,
            DEFAULT_CORS_ALLOW_CREDENTIALS);
    }

    /**
     * Get standard JAX-RS providers that are used in the deployment with optional realm extraction from the request
     * path and custom CORS behaviour
     */
    public static List<Object> getStandardProviders(
            boolean devMode,
            Integer realmIndex,
            Set<String> corsAllowedOrigins,
            String corsAllowedMethods,
            String corsExposedHeaders,
            int corsMaxAge,
            boolean corsAllowCredentials) {

        List<Object> providers = Lists.newArrayList(
           new RequestLogger(),
           new WebServiceExceptions.DefaultResteasyExceptionMapper(devMode),
           new WebServiceExceptions.ForbiddenResteasyExceptionMapper(devMode),
           new WebServiceExceptions.ServletUndertowExceptionHandler(devMode),
           new JacksonConfig(),
           new ClientErrorExceptionHandler(),
           new GZIPEncodingInterceptor(),
           new GZIPDecodingInterceptor(),
           new AcceptEncodingGZIPFilter()
        );

        if (realmIndex != null) {
           providers.addFirst(new RealmPathExtractorFilter(realmIndex));
        }

        if (corsAllowedOrigins != null) {
           CorsFilter corsFilter = new CorsFilter();
           corsFilter.getAllowedOrigins().addAll(corsAllowedOrigins);
           corsFilter.setAllowedMethods(corsAllowedMethods);
           corsFilter.setExposedHeaders(corsExposedHeaders);
           corsFilter.setCorsMaxAge(corsMaxAge);
           corsFilter.setAllowCredentials(corsAllowCredentials);
           providers.add(corsFilter);
        }

        return providers;
    }

    /**
     * Provides the LAN IPv4 address the container is bound to so it can be
     * used in the context provider callbacks; if CB is on the other side of some sort
     * of NAT then this won't work also assumes HTTP
     */
    public URI getHostUri() {
        return containerHostUri;
    }

    protected Undertow.Builder build(Container container, Undertow.Builder builder) {

        HttpHandler handler = new WebServiceExceptions.RootUndertowExceptionHandler(devMode, pathHandler);

        if (getBoolean(container.getConfig(), OR_WEBSERVER_DUMP_REQUESTS, OR_WEBSERVER_DUMP_REQUESTS_DEFAULT)) {
            handler = new RequestDumpingHandler(handler);
        }

        builder.setHandler(handler);

        return builder;
    }

   public ResteasyDeployment createResteasyDeployment(Application application, boolean secure) {
      ResteasyDeployment resteasyDeployment = new ResteasyDeploymentImpl();
      resteasyDeployment.setApplication(application);
      if (secure) {
         if (identityService == null) {
            throw new RuntimeException("Role based security can only be enabled when an identity service is available");
         }
         resteasyDeployment.setSecurityEnabled(true);
         // TODO: Use servlet security once implemented in identity service
         //identityService.secureDeployment(resteasyDeployment);
      }
      return resteasyDeployment;
   }

   public DeploymentInfo createDeploymentInfo(ResteasyDeployment resteasyDeployment, String deploymentPath, String deploymentName, Integer realmIndex, boolean secure) {

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


      // TODO: Remove this handler wrapper once JAX-RS RealmPathExtractorFilter can be utilised before security is applied
      if (realmIndex != null) {
         deploymentInfo.addInitialHandlerChainWrapper(handler -> {

            return exchange -> {
               // Do nothing if the realm header is already set
               if (exchange.getRequestHeaders().contains(REALM_PARAM_NAME)) {
                  handler.handleRequest(exchange);
                  return;
               }

               String relativePath = exchange.getRelativePath();
               StringBuilder newRelativePathBuilder = new StringBuilder();
               String realm = null;
               int segmentIndex = 0;
               int start = 1; // Path starts with '/'

               for (int i = 1; i <= relativePath.length(); i++) {
                  if (i == relativePath.length() || relativePath.charAt(i) == '/') {
                     if (i > start) { // Found a segment
                        if (segmentIndex == realmIndex) {
                           realm = relativePath.substring(start, i);
                        } else {
                           newRelativePathBuilder.append('/').append(relativePath, start, i);
                        }
                        segmentIndex++;
                     }
                     start = i + 1;
                  }
               }

               if (realm != null) {
                  exchange.getRequestHeaders().put(HttpString.tryFromString(REALM_PARAM_NAME), realm);

                  String newRelativePath = !newRelativePathBuilder.isEmpty() ? newRelativePathBuilder.toString() : "/";
                  String newRequestPath = deploymentInfo.getContextPath() + newRelativePath;
                  exchange.setRequestURI(newRequestPath);
                  exchange.setRelativePath(newRelativePath);
                  exchange.setRequestPath(newRequestPath);
               }

               handler.handleRequest(exchange);
            };
         });
      }

      if (secure) {
         if (identityService == null)
            throw new IllegalStateException(
               "No identity service found, make sure " + IdentityService.class.getName() + " is added before this service"
            );
         identityService.secureDeployment(deploymentInfo);
      }

      // This will catch anything not handled by Resteasy/Servlets, such as IOExceptions "at the wrong time"
      deploymentInfo.setExceptionHandler(new WebServiceExceptions.ServletUndertowExceptionHandler(devMode));
      return deploymentInfo;
   }

   public DeploymentInfo createFilesDeploymentInfo(ResourceManager resourceManager, String deploymentPath, String deploymentName, boolean devMode, String[] requiredRoles) {
       //FileServlet fileServlet = new FileServlet(devMode, resourceManager, requiredRoles);
       //ServletInfo servletInfo = Servlets.servlet("Manager File Servlet", FileServlet.class, () -> new ImmediateInstanceHandle<>(fileServlet));
       //servletInfo.addMapping("/*");

       Filter gzipFilter = new GzipResponseFilter(MIME_TYPES_TO_ZIP);
       FilterInfo gzipFilterInfo = Servlets.filter("Gzip Filter", GzipResponseFilter.class, () -> new ImmediateInstanceHandle<>(gzipFilter))
               .setAsyncSupported(true);

       return new DeploymentInfo()
               .setDeploymentName(deploymentName)
               .setResourceManager(resourceManager)
               .setContextPath(deploymentPath)
               .addServlet(Servlets.servlet("DefaultServlet", DefaultServlet.class))
               .addFilter(gzipFilterInfo)
               .addFilterUrlMapping(gzipFilterInfo.getName(), "/*", DispatcherType.REQUEST)
               .addWelcomePages("index.html", "index.htm")
               .setClassLoader(Container.class.getClassLoader());
   }

    public Undertow getUndertow() {
        return undertow;
    }

    public static List<String> getExternalHostnames(Container container) {

        // Get list of external hostnames
        String defaultHostname = getString(container.getConfig(), OR_HOSTNAME, null);
        String additionalHostnamesStr = getString(container.getConfig(), OR_ADDITIONAL_HOSTNAMES, null);

        List<String> externalHostnames = new ArrayList<>();

        if (!TextUtil.isNullOrEmpty(additionalHostnamesStr)) {
            externalHostnames.addAll(Arrays.stream(additionalHostnamesStr.split(","))
                .toList());
        }

        if (!TextUtil.isNullOrEmpty(defaultHostname) && !externalHostnames.contains(defaultHostname)) {
            externalHostnames.add(defaultHostname);
        }

        return externalHostnames;
    }

    public static Set<String> getCORSAllowedOrigins(Container container) {
        // Set allowed origins using external hostnames and WEBSERVER_ALLOWED_ORIGINS
        Set<String> allowedOrigins = new HashSet<>(
            getExternalHostnames(container)
                .stream().map(hostname -> "https://" + hostname).toList()
        );
        String allowedOriginsStr = getString(container.getConfig(), OR_WEBSERVER_ALLOWED_ORIGINS, null);

        if (!TextUtil.isNullOrEmpty(allowedOriginsStr)) {
            allowedOrigins.addAll(Arrays.stream(allowedOriginsStr.split(",")).toList());
        }

        return allowedOrigins;
    }
}
