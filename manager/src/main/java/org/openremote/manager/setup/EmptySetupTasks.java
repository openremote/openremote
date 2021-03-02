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
package org.openremote.manager.setup;

import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.model.Container;

import java.util.ArrayList;
import java.util.List;

/**
 * Just prepares keycloak identity provider if it is enabled
 */
public class EmptySetupTasks implements SetupTasks {

    final protected List<Setup> tasks = new ArrayList<>();

    protected void addTask(Setup task) {
        tasks.add(task);
    }

    public List<Setup> getTasks() {
        return tasks;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S extends Setup> S getTaskOfType(Class<S> setupType) {
        for (Setup task : tasks) {
            if (setupType.isAssignableFrom(task.getClass()))
                return (S) task;
        }
        throw new IllegalStateException("No setup task found of type: " + setupType);
    }

    @Override
    public List<Setup> createTasks(Container container) {
        // Basic vs Keycloak identity provider
        if (container.getService(ManagerIdentityService.class).isKeycloakEnabled()) {

            // Keycloak is not managed by persistence service so need separate clean task
            addTask(new KeycloakCleanSetup(container));
            addTask(new KeycloakInitSetup(container));
        }

        return getTasks();
    }
}
