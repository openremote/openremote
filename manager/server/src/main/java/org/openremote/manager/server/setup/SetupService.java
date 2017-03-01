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
package org.openremote.manager.server.setup;

import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.manager.server.setup.builtin.BuiltinSetupTasks;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Logger;

/**
 * Executes setup tasks when the application starts, configuring the state of
 * subsystems (database, Keycloak).
 * <p>
 * First, this service will load an implementation of {@link SetupTasks} from the
 * classpath using {@link ServiceLoader}. If multiple providers are found, an error
 * is raised. If a provider is found, only its tasks will be enabled.
 * <p>
 * If no {@link SetupTasks} provider is found on the classpath, the builtin
 * tasks will be enabled, see {@link BuiltinSetupTasks}.
 */
public class SetupService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(SetupService.class.getName());

    final protected List<Setup> setupList = new ArrayList<>();

    // Make demo/test data accessible from Groovy/Java code
    protected SetupTasks setupTasks;

    @Override
    public void init(Container container) throws Exception {
        // Setup runs last, when everything is initialized and started
    }

    @Override
    public void start(Container container) {

        ServiceLoader.load(SetupTasks.class).forEach(
            discoveredSetupTasks -> {
                if (setupTasks != null) {
                    throw new IllegalStateException(
                        "Only one instance of SetupTasks can be configured, already found '"
                            + setupTasks.getClass().getName() + ", remove from classpath: "
                            + discoveredSetupTasks.getClass().getName()
                    );
                }
                LOG.info("Enabling setup tasks: " + discoveredSetupTasks.getClass().getName());
                setupTasks = discoveredSetupTasks;
            }
        );

        if (setupTasks == null) {
            LOG.info("No custom setup tasks found on classpath, enabling built-in tasks");
            setupTasks = new BuiltinSetupTasks();
        }

        setupList.addAll(setupTasks.createTasks(container));

        try {
            if (setupList.size() > 0) {
                LOG.info("--- EXECUTING SETUP TASKS ---");
                for (Setup setup : setupList) {
                    setup.execute();
                }
                LOG.info("--- SETUP TASKS COMPLETED SUCCESSFULLY ---");
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error setting up application", ex);
        }
    }

    @Override
    public void stop(Container container) {
    }

    public <S extends Setup> S getTaskOfType(Class<S> setupType) {
        return setupTasks.getTaskOfType(setupType);
    }
}
