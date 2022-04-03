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
package org.openremote.model.setup;

import org.openremote.model.Container;

import java.util.List;

/**
 * Provides a list of {@link Setup} tasks to execute.
 */
public interface SetupTasks {

    /**
     * Can be used by setup tasks to have different configurable setups (e.g. production/staging/test/etc.)
     */
    String OR_SETUP_TYPE = "OR_SETUP_TYPE";

    List<Setup> createTasks(Container container, String setupType, boolean keycloakEnabled);
}
