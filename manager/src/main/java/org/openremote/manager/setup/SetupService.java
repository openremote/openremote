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
package org.openremote.manager.setup;

import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.security.IdentityService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.setup.builtin.BuiltinSetupTasks;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Logger;

/**
 * Executes setup tasks for a clean installation when the application starts.
 * <p>
 * This service is disabled when {@link PersistenceService#SETUP_WIPE_CLEAN_INSTALL} is <code>false</code>.
 * <p>
 * First, this service will load an implementation of {@link SetupTasks} from the
 * classpath using {@link ServiceLoader}. If multiple providers are found, an error
 * is raised. If a provider is found, only its tasks will be used.
 * <p>
 * If no {@link SetupTasks} provider is found on the classpath, the {@link BuiltinSetupTasks} are used.
 */
public class SetupService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(SetupService.class.getName());

    final protected List<Setup> setupList = new ArrayList<>();

    // Make demo/test data accessible from Groovy/Java code
    public SetupTasks setupTasks;

    @Override
    public int getPriority() {
        return PersistenceService.PRIORITY + 10; // Start just after persistence service
    }

    @Override
    public void init(Container container) throws Exception {

        boolean isClean = container.getService(PersistenceService.class).isSetupWipeCleanInstall();

        if (!isClean) {
            LOG.info("Setup service disabled, " + PersistenceService.SETUP_WIPE_CLEAN_INSTALL + "=false");
            return;
        }

        ServiceLoader.load(SetupTasks.class).forEach(
            discoveredSetupTasks -> {
                if (setupTasks != null) {
                    throw new IllegalStateException(
                        "Only one provider of SetupTasks can be configured, already found '"
                            + setupTasks.getClass().getName() + ", remove from classpath: "
                            + discoveredSetupTasks.getClass().getName()
                    );
                }
                LOG.info("Found custom SetupTasks provider on classpath: " + discoveredSetupTasks.getClass().getName());
                setupTasks = discoveredSetupTasks;
            }
        );

        if (setupTasks == null) {
            LOG.info("No custom SetupTasks provider found on classpath, enabling: " + BuiltinSetupTasks.class.getName());
            setupTasks = new BuiltinSetupTasks();
        }

        setupList.addAll(setupTasks.createTasks(container));

        try {
            if (setupList.size() > 0) {
                LOG.info("--- EXECUTING INIT TASKS ---");
                for (Setup setup : setupList) {
                    setup.onInit();
                }
                LOG.info("--- INIT TASKS COMPLETED SUCCESSFULLY ---");
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error setting up application", ex);
        }
    }

    @Override
    public void start(Container container) {

        try {
            if (setupList.size() > 0) {
                LOG.info("--- EXECUTING START TASKS ---");
                for (Setup setup : setupList) {
                    setup.onStart();
                }
                LOG.info("--- START TASKS COMPLETED SUCCESSFULLY ---");
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error setting up application", ex);
        }

        // When setup is complete, initialize the clock again - effectively set the PSEUDO clock to
        // wall clock time. If the PSEUDO clock is enabled in tests, we must advance time after setup
        // imports asset data. The protocols which are started after setup can trigger asset attribute
        // events, and event source time must be later than asset state time.
        container.getService(TimerService.class).getClock().init();
    }

    @Override
    public void stop(Container container) {
    }

    public <S extends Setup> S getTaskOfType(Class<S> setupType) {
        return setupTasks.getTaskOfType(setupType);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            '}';
    }
}
