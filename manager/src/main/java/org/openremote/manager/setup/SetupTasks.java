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

import org.openremote.container.Container;

import java.util.List;

import static org.openremote.container.util.MapAccess.getBoolean;

/**
 * Provides a list of enabled {@link Setup} tasks.
 * <p>
 * Implementations use the static getter methods of this interface to query configuration, before returning a
 * list of their enabled tasks in {@link #createTasks}.
 * <p>
 * Configuration options can have dependencies, enabling one option may enforce other options to be enabled.
 * <p>
 * The configuration options exposed as container environment variables are:
 * <dl>
 * <dt>{@link #SETUP_WIPE_CLEAN_INSTALL}</dt>
 * <dd>
 * Defaults to enabled if {@link Container#DEV_MODE} is enabled. All setup tasks will be ignored if this option is disabled.
 * </dd>
 * <dt>{@link #SETUP_IMPORT_DEMO_USERS}</dt>
 * <dd>
 * Defaults to enabled if {@link Container#DEV_MODE} is enabled.
 * </dd>
 * <dt>{@link #SETUP_IMPORT_DEMO_ASSETS}</dt>
 * <dd>
 * Defaults to enabled if {@link Container#DEV_MODE} is enabled, enables {@link #SETUP_IMPORT_DEMO_USERS}.
 * </dd>
 * <dt>{@link #SETUP_IMPORT_DEMO_RULES}</dt>
 * <dd>
 * Defaults to enabled if {@link Container#DEV_MODE} is enabled, enables {@link #SETUP_IMPORT_DEMO_ASSETS}.
 * </dd>
 * <dt>{@link #SETUP_IMPORT_DEMO_SCENES}</dt>
 * <dd>
 * Defaults to enabled if {@link Container#DEV_MODE} is enabled, enables {@link #SETUP_IMPORT_DEMO_ASSETS}.
 * </dd>
 * <dt>{@link #SETUP_IMPORT_DEMO_AGENT}</dt>
 * <dd>
 * Defaults to disabled even when {@link Container#DEV_MODE} is enabled (as enabling may require
 * protocol-specific hardware), enables {@link #SETUP_IMPORT_DEMO_ASSETS}.
 * </dd>
 * </dl>
 */
public interface SetupTasks {


    String SETUP_IMPORT_DEMO_USERS = "SETUP_IMPORT_DEMO_USERS";
    String SETUP_IMPORT_DEMO_ASSETS = "SETUP_IMPORT_DEMO_ASSETS";
    String SETUP_IMPORT_DEMO_RULES = "SETUP_IMPORT_DEMO_RULES";
    String SETUP_IMPORT_DEMO_SCENES = "SETUP_IMPORT_DEMO_SCENES";
    String SETUP_IMPORT_DEMO_AGENT = "SETUP_IMPORT_DEMO_AGENT";

    static boolean isImportDemoUsers(Container container) {
        return isImportDemoAssets(container)
            || getBoolean(container.getConfig(), SETUP_IMPORT_DEMO_USERS, container.isDevMode());
    }

    static boolean isImportDemoAssets(Container container) {
        return isImportDemoAgent(container)
            || isImportDemoRules(container)
            || isImportDemoScenes(container)
            || getBoolean(container.getConfig(), SETUP_IMPORT_DEMO_ASSETS, container.isDevMode());
    }

    static boolean isImportDemoRules(Container container) {
        return getBoolean(container.getConfig(), SETUP_IMPORT_DEMO_RULES, container.isDevMode());
    }

    static boolean isImportDemoScenes(Container container) {
        return getBoolean(container.getConfig(), SETUP_IMPORT_DEMO_SCENES, container.isDevMode());
    }

    // Defaults to always disabled as agents might require actual protocol-specific hardware
    static boolean isImportDemoAgent(Container container) {
        return getBoolean(container.getConfig(), SETUP_IMPORT_DEMO_AGENT, false);
    }

    List<Setup> createTasks(Container container);

    <S extends Setup> S getTaskOfType(Class<S> setupType);
}
