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

/**
 * Test setup tasks.
 */
public class TestSetupTasks extends EmptySetupTasks {

    public static final String SETUP_CREATE_USERS = "SETUP_CREATE_USERS";
    public static final String SETUP_CREATE_ASSETS = "SETUP_CREATE_ASSETS";
    public static final String SETUP_CREATE_RULES = "SETUP_CREATE_RULES";

    static boolean isImportUsers(Container container) {
        return isImportAssets(container)
            || getBoolean(container.getConfig(), SETUP_CREATE_USERS, container.isDevMode());
    }

    static boolean isImportAssets(Container container) {
        return isImportRules(container)
            || getBoolean(container.getConfig(), SETUP_CREATE_ASSETS, container.isDevMode());
    }

    static boolean isImportRules(Container container) {
        return getBoolean(container.getConfig(), SETUP_CREATE_RULES, container.isDevMode());
    }

    @Override
    public List<Setup> createTasks(Container container) {
        super.createTasks(container);

        // Basic vs Keycloak identity provider
        if (container.getService(ManagerIdentityService.class).isKeycloakEnabled()) {

            if (isImportUsers(container)) {
                addTask(new KeycloakTestSetup(container));
            }

            if (isImportAssets(container)) {
                addTask(new ManagerTestSetup(container));
            }

            if (isImportRules(container)) {
                addTask(new RulesTestSetup(container));
            }

            addTask(new ManagerTestAgentSetup(container));
        }

        return getTasks();
    }
}
