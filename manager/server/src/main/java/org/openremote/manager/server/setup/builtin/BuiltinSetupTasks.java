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
 * <li>{@link #SETUP_INIT_CLEAN_DATABASE}</li>
 * <li>{@link #SETUP_IMPORT_DEMO_USERS}</li>
 * <li>{@link #SETUP_IMPORT_DEMO_ASSETS} depends on {@link #SETUP_IMPORT_DEMO_USERS}</li>
 * <li>{@link #SETUP_IMPORT_DEMO_RULES} depends on {@link #SETUP_IMPORT_DEMO_ASSETS}</li>
 * <li>{@link #SETUP_IMPORT_DEMO_SCENES} depends on {@link #SETUP_IMPORT_DEMO_RULES}</li>
 * </ul>
 */
public class BuiltinSetupTasks extends AbstractSetupTasks {

    public static final String SETUP_INIT_CLEAN_DATABASE = "SETUP_INIT_CLEAN_DATABASE";
    public static final String SETUP_IMPORT_DEMO_USERS = "SETUP_IMPORT_DEMO_USERS";
    public static final String SETUP_IMPORT_DEMO_ASSETS = "SETUP_IMPORT_DEMO_ASSETS";
    public static final String SETUP_IMPORT_DEMO_RULES = "SETUP_IMPORT_DEMO_RULES";
    public static final String SETUP_IMPORT_DEMO_SCENES = "SETUP_IMPORT_DEMO_SCENES";

    @Override
    public List<Setup> createTasks(Container container) {

        boolean cleanDatabase = getBoolean(container.getConfig(), SETUP_INIT_CLEAN_DATABASE, container.isDevMode());

        // Demo scenes are optional because they create many attributes
        boolean importDemoScenes = getBoolean(container.getConfig(), SETUP_IMPORT_DEMO_SCENES, container.isDevMode());

        // If importing demo scenes we have to import demo rules
        boolean importDemoRules = importDemoScenes || getBoolean(container.getConfig(), SETUP_IMPORT_DEMO_RULES, container.isDevMode());

        // If importing demo rules we have to import demo assets
        boolean importDemoAssets = importDemoRules || getBoolean(container.getConfig(), SETUP_IMPORT_DEMO_ASSETS, container.isDevMode());

        // If importing demo assets we have to import demo users
        boolean importDemoUsers = importDemoAssets || getBoolean(container.getConfig(), SETUP_IMPORT_DEMO_USERS, container.isDevMode());

        if (cleanDatabase) {
            addTask(new ManagerCleanSetup(container));
            addTask(new KeycloakCleanSetup(container));
            addTask(new KeycloakInitSetup(container));
            addTask(new ManagerInitSetup(container));
        }

        if (importDemoUsers) {
            addTask(new KeycloakDemoSetup(container));
        }

        if (importDemoAssets) {
            addTask(new ManagerDemoSetup(container, importDemoScenes));
        }

        if (importDemoRules) {
            addTask(new RulesDemoSetup(container));
        }

        return getTasks();
    }
}
