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

import org.apache.log4j.Logger;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;

import java.util.ArrayList;
import java.util.List;

import static org.openremote.container.util.MapAccess.getBoolean;

/**
 * Executes setup tasks when the application starts, configuring the state of
 * subsystems (database, Keycloak).
 * <p>
 * When developer mode is enabled, all setup tasks will be executed regardless of
 * other configuration options. With developer mode disabled, tasks can be selectively
 * enabled (they are disabled by default), see:
 * <ul>
 * <li>{@link #SETUP_CLEAN_INIT_KEYCLOAK}</li>
 * <li>{@link #SETUP_CLEAN_INIT_MANAGER}</li>
 * <li>{@link #SETUP_IMPORT_DEMO_DATA}</li>
 * </ul>
 */
public class SetupService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(SetupService.class.getName());

    public static final String SETUP_CLEAN_INIT_KEYCLOAK = "SETUP_CLEAN_INIT_KEYCLOAK";
    public static final boolean SETUP_CLEAN_INIT_KEYCLOAK_DEFAULT = false;

    public static final String SETUP_CLEAN_INIT_MANAGER = "SETUP_CLEAN_INIT_MANAGER";
    public static final boolean SETUP_CLEAN_INIT_MANAGER_DEFAULT = false;

    public static final String SETUP_IMPORT_DEMO_DATA = "SETUP_IMPORT_DEMO_DATA";
    public static final boolean SETUP_IMPORT_DEMO_DATA_DEFAULT = false;

    final protected List<Setup> setupList = new ArrayList<>();

    // Make demo/test data accessible from Groovy/Java code
    protected KeycloakDemoSetup keycloakDemoSetup;
    protected ManagerDemoSetup managerDemoSetup;

    @Override
    public void init(Container container) throws Exception {
        // Setup runs last, when everything is initialized and started
    }

    @Override
    public void start(Container container) {
        if (container.isDevMode()) {

            setupList.add(new KeycloakCleanSetup(container));
            setupList.add(new KeycloakInitSetup(container));
            keycloakDemoSetup = new KeycloakDemoSetup(container);
            setupList.add(keycloakDemoSetup);

            setupList.add(new ManagerCleanSetup(container));
            setupList.add(new ManagerInitSetup(container));
            managerDemoSetup = new ManagerDemoSetup(container);
            setupList.add(managerDemoSetup);

        } else {

            if (getBoolean(container.getConfig(), SETUP_CLEAN_INIT_KEYCLOAK, SETUP_CLEAN_INIT_KEYCLOAK_DEFAULT)) {
                setupList.add(new KeycloakCleanSetup(container));
                setupList.add(new KeycloakInitSetup(container));
            }
            if (getBoolean(container.getConfig(), SETUP_IMPORT_DEMO_DATA, SETUP_IMPORT_DEMO_DATA_DEFAULT)) {
                keycloakDemoSetup = new KeycloakDemoSetup(container);
                setupList.add(keycloakDemoSetup);
            }

            if (getBoolean(container.getConfig(), SETUP_CLEAN_INIT_MANAGER, SETUP_CLEAN_INIT_MANAGER_DEFAULT)) {
                setupList.add(new ManagerCleanSetup(container));
                setupList.add(new ManagerInitSetup(container));
            }
            if (getBoolean(container.getConfig(), SETUP_IMPORT_DEMO_DATA, SETUP_IMPORT_DEMO_DATA_DEFAULT)) {
                managerDemoSetup = new ManagerDemoSetup(container);
                setupList.add(managerDemoSetup);
            }
        }

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

    public KeycloakDemoSetup getKeycloakDemoSetup() {
        if (keycloakDemoSetup == null)
            throw new IllegalStateException("Demo data not imported");
        return keycloakDemoSetup;
    }

    public ManagerDemoSetup getManagerDemoSetup() {
        if (managerDemoSetup == null)
            throw new IllegalStateException("Demo data not imported");
        return managerDemoSetup;
    }
}
