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
package org.openremote.manager.setup.builtin;

import org.openremote.container.Container;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.setup.AbstractSetupTasks;
import org.openremote.manager.setup.Setup;

import java.util.List;

import static org.openremote.manager.setup.SetupTasks.*;

/**
 * Builtin setup tasks.
 */
public class BuiltinSetupTasks extends AbstractSetupTasks {

    @Override
    public List<Setup> createTasks(Container container) {

        // Basic vs Keycloak identity provider
        if (container.getService(ManagerIdentityService.class).isKeycloakEnabled()) {
            // Keycloak is not managed by persistence service so need separate clean task
            addTask(new KeycloakCleanSetup(container));
            addTask(new KeycloakInitSetup(container));
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
        }

        return getTasks();
    }
}
