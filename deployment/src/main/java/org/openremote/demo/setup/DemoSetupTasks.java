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
package org.openremote.demo.setup;

import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.setup.EmptySetupTasks;
import org.openremote.manager.setup.Setup;
import org.openremote.manager.setup.SetupTasks;
import org.openremote.model.Container;

import java.util.List;

import static org.openremote.manager.setup.SetupTasks.*;

/**
 * Builtin setup tasks.
 */
public class DemoSetupTasks extends EmptySetupTasks {

    @Override
    public List<Setup> createTasks(Container container) {
        super.createTasks(container);

        // Basic vs Keycloak identity provider
        if (container.getService(ManagerIdentityService.class).isKeycloakEnabled()) {

            if (SetupTasks.isImportDemoUsers(container)) {
                addTask(new KeycloakDemoSetup(container));
            }
            if (SetupTasks.isImportDemoAssets(container)) {
                addTask(new ManagerDemoSetup(container));
            }
            if (SetupTasks.isImportDemoRules(container)) {
                addTask(new RulesDemoSetup(container));
            }
            if (SetupTasks.isImportDemoAgent(container)) {
                addTask(new ManagerDemoAgentSetup(container));
            }
        }

        return getTasks();
    }
}
