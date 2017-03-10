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
package org.openremote.manager.server.web;

import io.undertow.server.HttpHandler;
import io.undertow.servlet.api.DeploymentInfo;
import org.openremote.container.Container;
import org.openremote.container.security.IdentityService;
import org.openremote.container.web.WebService;
import org.openremote.model.Constants;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static org.openremote.container.util.MapAccess.getString;

public class ManagerWebService extends WebService {

    private static final Logger LOG = Logger.getLogger(ManagerWebService.class.getName());

    public static final String MANAGER_DOCROOT = "MANAGER_DOCROOT";
    public static final String MANAGER_DOCROOT_DEFAULT = "manager/client/src/main/webapp";
    public static final String CONSOLE_DOCROOT = "CONSOLE_DOCROOT";
    public static final String CONSOLE_DOCROOT_DEFAULT = "deployment/manager/resources_console";

    public static final String MANAGER_PATH = "/static";
    public static final String CONSOLE_PATH = "/console";
    public static final Pattern MANAGER_PATTERN = Pattern.compile(Pattern.quote(MANAGER_PATH) + "(/.*)?");
    public static final Pattern CONSOLE_PATTERN = Pattern.compile(Pattern.quote(CONSOLE_PATH) + "(/.*)?");

    protected HttpHandler managerFileHandler;

    @Override
    public void init(Container container) throws Exception {

        // Serve the Manager client files unsecured
        Path managerDocRoot = Paths.get(
            getString(container.getConfig(), MANAGER_DOCROOT, MANAGER_DOCROOT_DEFAULT)
        );
        DeploymentInfo managerDeployment = ManagerFileServlet.createDeploymentInfo(
            container.isDevMode(), MANAGER_PATH, managerDocRoot, new String[0] // Unsecured, no required roles!
        );
        managerFileHandler = addServletDeployment(
            container.getService(IdentityService.class), managerDeployment, false
        );
        getPrefixRoutes().put(
            MANAGER_PATH, ManagerFileServlet.wrapHandler(managerFileHandler, MANAGER_PATTERN)
        );

        // Serve the Console client files unsecured
        Path consoleDocRoot = Paths.get(
            getString(container.getConfig(), CONSOLE_DOCROOT, CONSOLE_DOCROOT_DEFAULT)
        );
        DeploymentInfo consoleDeployment = ManagerFileServlet.createDeploymentInfo(
            container.isDevMode(), CONSOLE_PATH, consoleDocRoot, new String[0] // Unsecured, no required roles!
        );
        HttpHandler consoleHandler = addServletDeployment(
            container.getService(IdentityService.class), consoleDeployment, false
        );
        getPrefixRoutes().put(
            CONSOLE_PATH, ManagerFileServlet.wrapHandler(consoleHandler, CONSOLE_PATTERN)
        );

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

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            '}';
    }

}
