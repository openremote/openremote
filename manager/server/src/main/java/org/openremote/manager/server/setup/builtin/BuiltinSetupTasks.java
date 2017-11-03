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
package org.openremote.manager.server.setup.builtin;

import org.openremote.container.Container;
import org.openremote.manager.server.security.ManagerIdentityService;
import org.openremote.manager.server.setup.AbstractSetupTasks;
import org.openremote.manager.server.setup.Setup;

import java.util.List;

import static org.openremote.container.util.MapAccess.getBoolean;

/**
 * Builtin setup tasks.
 * <p>
 * All setup tasks are configurable.
 * <p>
 * When developer mode is enabled all setup tasks default to enabled.
 * When developer mode is disabled all setup tasks default to false.
 * <p>
 * Some setup tasks require other setup tasks so if a dependent task
 * is requested to be disabled it will be overridden and enabled anyway
 * (see list of config options for details)
 * <p>
 * Setup tasks are configured via the config hash map supplied in the
 * {@link Container} constructor. Available config options are:
  * <ul>
 * <li>{@link #SETUP_WIPE_CLEAN_INSTALL}</li>
 * <li>{@link #SETUP_IMPORT_DEMO_USERS}</li>
 * <li>{@link #SETUP_IMPORT_DEMO_ASSETS} depends on {@link #SETUP_IMPORT_DEMO_USERS}</li>
 * <li>{@link #SETUP_IMPORT_DEMO_SCENES} depends on {@link #SETUP_IMPORT_DEMO_ASSETS}</li>
 * <li>{@link #SETUP_IMPORT_DEMO_RULES} depends on {@link #SETUP_IMPORT_DEMO_SCENES}</li>
 * </ul>
 */
public class BuiltinSetupTasks extends AbstractSetupTasks {

    public static final String SETUP_WIPE_CLEAN_INSTALL = "SETUP_WIPE_CLEAN_INSTALL";
    public static final String SETUP_IMPORT_DEMO_USERS = "SETUP_IMPORT_DEMO_USERS";
    public static final String SETUP_IMPORT_DEMO_ASSETS = "SETUP_IMPORT_DEMO_ASSETS";
    public static final String SETUP_IMPORT_DEMO_SCENES = "SETUP_IMPORT_DEMO_SCENES";
    public static final String SETUP_IMPORT_DEMO_RULES = "SETUP_IMPORT_DEMO_RULES";
    public static final String SETUP_IMPORT_DEMO_AGENT = "SETUP_IMPORT_DEMO_AGENT";

    protected boolean isSetupWipeCleanInstall(Container container) {
        return getBoolean(container.getConfig(), SETUP_WIPE_CLEAN_INSTALL, container.isDevMode());
    }

    // Demo agent to demo protocols.
    protected boolean isImportDemoAgent(Container container) {
        return getBoolean(container.getConfig(), SETUP_IMPORT_DEMO_AGENT, false);
    }

    // Too many rules are difficult to debug, so they are optional
    protected boolean isImportDemoRules(Container container) {
        return getBoolean(container.getConfig(), SETUP_IMPORT_DEMO_RULES, container.isDevMode());
    }

    // If importing demo rules we have to import demo scenes
    protected boolean isImportDemoScenes(Container container) {
        return isImportDemoRules(container) || getBoolean(container.getConfig(), SETUP_IMPORT_DEMO_SCENES, container.isDevMode());
    }

    // If importing demo rules we have to import demo assets
    protected boolean isImportDemoAssets(Container container) {
        return isImportDemoRules(container) || getBoolean(container.getConfig(), SETUP_IMPORT_DEMO_ASSETS, container.isDevMode());
    }

    // If importing demo assets we have to import demo users
    protected boolean isImportDemoUsers(Container container) {
        return isImportDemoAssets(container) || getBoolean(container.getConfig(), SETUP_IMPORT_DEMO_USERS, container.isDevMode());
    }

    @Override
    public List<Setup> createTasks(Container container) {

        // Basic vs Keycloak identity provider
        if (container.getService(ManagerIdentityService.class).isKeycloakEnabled()) {
            if (isSetupWipeCleanInstall(container)) {
                addTask(new ManagerCleanSetup(container));
                addTask(new KeycloakCleanSetup(container));
                addTask(new KeycloakInitSetup(container));
            }

            if (isImportDemoUsers(container)) {
                addTask(new KeycloakDemoSetup(container));
            }

            if (isImportDemoAssets(container)) {
                addTask(new ManagerDemoSetup(container, isImportDemoScenes(container)));
            }

            if (isImportDemoRules(container)) {
                addTask(new RulesDemoSetup(container));
            }
            
            if (isImportDemoAgent(container)) {
                addTask(new ManagerDemoAgentSetup(container));
            }
        } else {
            if (isSetupWipeCleanInstall(container)) {
                addTask(new ManagerCleanSetup(container));
                addTask(new BasicIdentityInitSetup(container));
            }

            if (isImportDemoUsers(container)) {
                addTask(new BasicIdentityDemoSetup(container));
            }
        }

        return getTasks();
    }
}
