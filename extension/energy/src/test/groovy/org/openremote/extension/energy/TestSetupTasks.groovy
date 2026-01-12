package org.openremote.extension.energy

import org.openremote.model.Container
import org.openremote.model.setup.Setup
import org.openremote.model.setup.SetupTasks

class TestSetupTasks implements SetupTasks {

    @Override
    List<Setup> createTasks(Container container, String setupType, boolean keycloakEnabled) {
        return List.of(new KeycloakTestSetup(container), new ManagerTestSetup(container))
    }
}
