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

import org.openremote.model.Container;
import org.openremote.model.setup.Setup;
import org.openremote.model.setup.SetupTasks;

import java.util.Arrays;
import java.util.List;

/**
 * Test setup tasks.
 */
public class TestSetupTasks implements SetupTasks {

    @Override
    public List<Setup> createTasks(Container container, String setupType, boolean keycloakEnabled) {
        return Arrays.asList(
            new KeycloakTestSetup(container),
            new ManagerTestSetup(container),
            new ManagerTestAgentSetup(container)
        );
    }
}
