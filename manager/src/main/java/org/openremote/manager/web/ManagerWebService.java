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
package org.openremote.manager.web;

import io.undertow.server.HttpHandler;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import org.openremote.container.Container;
import org.openremote.container.security.IdentityService;
import org.openremote.container.web.WebService;
import org.openremote.model.Constants;

import javax.ws.rs.core.UriBuilder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import static org.openremote.container.util.MapAccess.getBoolean;
import static org.openremote.container.util.MapAccess.getString;

public class ManagerWebService extends WebService {

    public static final String MANAGER_DOCROOT = "MANAGER_DOCROOT";
    public static final String MANAGER_DOCROOT_DEFAULT = "client/src/main/webapp";
    public static final String CONSOLES_DOCROOT = "CONSOLES_DOCROOT";
    public static final String CONSOLES_DOCROOT_DEFAULT = "deployment/manager/consoles";
    public static final String UI_DOCROOT = "UI_DOCROOT";
    public static final String UI_DOCROOT_DEFAULT = "deployment/manager/ui";
    public static final String CONSOLE_USE_STATIC_BOWER_COMPONENTS = "CONSOLE_USE_STATIC_BOWER_COMPONENTS";
    public static final boolean CONSOLE_USE_STATIC_BOWER_COMPONENTS_DEFAULT = true;

    public static final String MANAGER_PATH = "/static";
    public static final String CONSOLE_PATH = "/console";
    public static final String UI_PATH = "/ui";
    public static final Pattern MANAGER_PATTERN = Pattern.compile(Pattern.quote(MANAGER_PATH) + "(/.*)?");
    public static final Pattern CONSOLE_PATTERN = Pattern.compile(Pattern.quote(CONSOLE_PATH) + "(/.*)?");
    public static final Pattern UI_PATTERN = Pattern.compile(Pattern.quote(UI_PATH) + "(/.*)?");

    protected Path managerDocRoot;
    protected Path consolesDocRoot;
    protected Path uiDocRoot;
    protected HttpHandler managerFileHandler;

    @Override
    public void init(Container container) throws Exception {
        boolean devMode = container.isDevMode();
        IdentityService identityService = container.getService(IdentityService.class);

        // Serve the Manager client files unsecured
        managerDocRoot = Paths.get(getString(container.getConfig(), MANAGER_DOCROOT, MANAGER_DOCROOT_DEFAULT));
        managerFileHandler = addDeployment(devMode, identityService, managerDocRoot, MANAGER_PATH);
        addRoute(managerFileHandler, MANAGER_PATH, MANAGER_PATTERN);

        // Serve the Console client files unsecured
        consolesDocRoot = Paths.get(getString(container.getConfig(), CONSOLES_DOCROOT, CONSOLES_DOCROOT_DEFAULT));
        HttpHandler consoleHandler = addDeployment(devMode, identityService, consolesDocRoot, CONSOLE_PATH);

        final boolean useStaticBowerComponents =
            getBoolean(container.getConfig(), CONSOLE_USE_STATIC_BOWER_COMPONENTS, CONSOLE_USE_STATIC_BOWER_COMPONENTS_DEFAULT);
        // Special case for Console client files: When certain files are requested, serve them from the /static/*
        // resources already deployed in Manager. In other words: Console apps then can not have their own Polymer etc.
        // resources but use same libraries as the platform.
        String[] consoleStaticResources = {
            "/bower_components/polymer/polymer.html",
            "/bower_components/polymer/polymer-element.html",
            "/bower_components/iron-flex-layout/iron-flex-layout.html",
            "/bower_components/iron-flex-layout/iron-flex-layout-classes.html",
            "/bower_components/chart.js/dist/Chart.js",
            // TODO Add all the other stuff but Intl is many files, no idea how we deal with this... good approach?
        };
        addRoute(exchange -> {
                if (useStaticBowerComponents) {
                    for (String consoleStaticResource : consoleStaticResources) {
                        if (exchange.getRequestPath().endsWith(consoleStaticResource)) {
                            exchange.setStatusCode(StatusCodes.FOUND);
                            exchange.getResponseHeaders().put(Headers.LOCATION, "/static" + consoleStaticResource);
                            exchange.endExchange();
                            return;
                        }
                    }
                }
                consoleHandler.handleRequest(exchange);
            }, CONSOLE_PATH, CONSOLE_PATTERN
        );

        // Serve the UI deployment files unsecured
        uiDocRoot = Paths.get(getString(container.getConfig(), UI_DOCROOT, UI_DOCROOT_DEFAULT));
        addRoute(addDeployment(devMode, identityService, uiDocRoot, UI_PATH), UI_PATH, UI_PATTERN);

        super.init(container);
    }

    @Override
    public String getDefaultRealm() {
        return Constants.MASTER_REALM;
    }

    @Override
    protected HttpHandler getRealmIndexHandler() {
        return managerFileHandler;
    }

    public Path getManagerDocRoot() {
        return managerDocRoot;
    }

    public Path getConsolesDocRoot() {
        return consolesDocRoot;
    }

    public Path getUiDocRoot() {
        return uiDocRoot;
    }

    public String getConsoleUrl(UriBuilder baseUri, String realm) {
        return baseUri.path(CONSOLE_PATH).path(realm).build().toString();
    }

    protected HttpHandler addDeployment(boolean devMode, IdentityService identityService, Path filePath, String hostOnPath) {
        DeploymentInfo deploymentInfo = ManagerFileServlet.createDeploymentInfo(devMode, hostOnPath, filePath, new String[0]);
        return addServletDeployment(identityService, deploymentInfo, false);
    }

    protected void addRoute(HttpHandler httpHandler, String hostOnPath, Pattern requestPattern) {
        getPrefixRoutes().put(hostOnPath, ManagerFileServlet.wrapHandler(httpHandler, requestPattern));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "managerDocRoot=" + managerDocRoot +
            ", consolesDocRoot=" + consolesDocRoot +
            ", uiDocRoot=" + uiDocRoot +
            '}';
    }
}
