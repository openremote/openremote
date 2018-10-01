package org.openremote.manager.setup.builtin;

import org.openremote.container.Container;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.setup.AbstractSetupTasks;
import org.openremote.manager.setup.Setup;

import java.util.List;

/**
 * Java class description...
 * <p>
 * Date : 09-Aug-18
 *
 * @author jerome.vervier
 */
public class ControllerIntegrationSetupTasks extends AbstractSetupTasks {
    @Override
    public List<Setup> createTasks(Container container) {

        // Basic vs Keycloak identity provider
        if (container.getService(ManagerIdentityService.class).isKeycloakEnabled()) {
            addTask(new ManagerCleanSetup(container));
            addTask(new KeycloakCleanSetup(container));
            addTask(new KeycloakInitSetup(container));
            addTask(new KeycloakDemoSetup(container));
            addTask(new ManagerControllerIntegrationSetup(container));
        } else {
            addTask(new ManagerCleanSetup(container));
            addTask(new BasicIdentityInitSetup(container));
            addTask(new ManagerControllerIntegrationSetup(container));
        }

        return getTasks();
    }
}
