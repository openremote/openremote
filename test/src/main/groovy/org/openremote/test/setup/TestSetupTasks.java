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
package org.openremote.test.setup;

import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.setup.EmptySetupTasks;
import org.openremote.manager.setup.Setup;
import org.openremote.model.Container;

import java.util.List;

import static org.openremote.container.util.MapAccess.getBoolean;
import static org.openremote.manager.setup.SetupTasks.*;

/**
 * Builtin setup tasks.
 */
public class TestSetupTasks extends EmptySetupTasks {

    public static final String SETUP_CREATE_SCENES = "SETUP_CREATE_SCENES";
    public static final String SETUP_CREATE_USERS = "SETUP_CREATE_USERS";
    public static final String SETUP_CREATE_ASSETS = "SETUP_CREATE_ASSETS";
    public static final String SETUP_CREATE_RULES = "SETUP_CREATE_RULES";
    public static final String SETUP_IMPORT_DEMO_AGENT = "SETUP_CREATE_AGENT";

    static boolean isImportDemoUsers(Container container) {
        return isImportDemoAssets(container)
            || getBoolean(container.getConfig(), SETUP_CREATE_USERS, container.isDevMode());
    }

    static boolean isImportDemoAssets(Container container) {
        return isImportDemoAgent(container)
            || isImportDemoRules(container)
            || getBoolean(container.getConfig(), SETUP_CREATE_ASSETS, container.isDevMode());
    }

    static boolean isImportDemoRules(Container container) {
        return getBoolean(container.getConfig(), SETUP_CREATE_RULES, container.isDevMode());
    }

    // Defaults to always disabled as agents might require actual protocol-specific hardware
    static boolean isImportDemoAgent(Container container) {
        return getBoolean(container.getConfig(), SETUP_IMPORT_DEMO_AGENT, false);
    }
    static boolean isImportDemoScenes(Container container) {
        return getBoolean(container.getConfig(), SETUP_CREATE_SCENES, container.isDevMode());
    }

    @Override
    public List<Setup> createTasks(Container container) {
        super.createTasks(container);

        // Basic vs Keycloak identity provider
        if (container.getService(ManagerIdentityService.class).isKeycloakEnabled()) {

            if (isImportDemoUsers(container)) {
                addTask(new KeycloakTestSetup(container));
            }

            if (isImportDemoAssets(container) || isImportDemoScenes(container)) {
                addTask(new ManagerTestSetup(container, isImportDemoScenes(container)));
            }

            if (isImportDemoRules(container)) {
                addTask(new RulesTestSetup(container));
            }

            if (isImportDemoAgent(container)) {
                addTask(new ManagerTestAgentSetup(container));
            }
        }

        return getTasks();
    }
}
