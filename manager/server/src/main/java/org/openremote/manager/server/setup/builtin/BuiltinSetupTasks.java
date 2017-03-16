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
 * When developer mode is enabled, all builtin setup tasks will be executed
 * regardless of other configuration options. With developer mode disabled,
 * builtin tasks can be selectively enabled (they are disabled by default), see:
 * <ul>
 * <li>{@link #SETUP_INIT_CLEAN_DATABASE}</li>
 * <li>{@link #SETUP_IMPORT_DEMO_DATA}</li>
 * </ul>
 */
public class BuiltinSetupTasks extends AbstractSetupTasks {

    public static final String SETUP_INIT_CLEAN_DATABASE = "SETUP_INIT_CLEAN_DATABASE";
    public static final boolean SETUP_INIT_CLEAN_DATABASE_DEFAULT = false;

    public static final String SETUP_IMPORT_DEMO_DATA = "SETUP_IMPORT_DEMO_DATA";
    public static final boolean SETUP_IMPORT_DEMO_DATA_DEFAULT = false;

    @Override
    public List<Setup> createTasks(Container container) {

        if (container.isDevMode()) {

            addTask(new ManagerCleanSetup(container));
            addTask(new KeycloakCleanSetup(container));
            addTask(new KeycloakInitSetup(container));
            addTask(new ManagerInitSetup(container));

            addTask(new KeycloakDemoSetup(container));
            addTask(new ManagerDemoSetup(container));

        } else {

            if (getBoolean(container.getConfig(), SETUP_INIT_CLEAN_DATABASE, SETUP_INIT_CLEAN_DATABASE_DEFAULT)) {
                addTask(new ManagerCleanSetup(container));
                addTask(new KeycloakCleanSetup(container));
                addTask(new KeycloakInitSetup(container));
                addTask(new ManagerInitSetup(container));
            }

            if (getBoolean(container.getConfig(), SETUP_IMPORT_DEMO_DATA, SETUP_IMPORT_DEMO_DATA_DEFAULT)) {
                addTask(new KeycloakDemoSetup(container));
                addTask(new ManagerDemoSetup(container));
            }

        }

        return getTasks();
    }
}
