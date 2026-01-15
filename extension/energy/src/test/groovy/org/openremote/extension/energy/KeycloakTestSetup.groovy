package org.openremote.extension.energy

import org.openremote.manager.setup.AbstractKeycloakSetup
import org.openremote.model.Container
import org.openremote.model.security.Realm

class KeycloakTestSetup extends AbstractKeycloakSetup {

    Realm realmEnergy

    KeycloakTestSetup(Container container) {
        super(container)
    }

    @Override
    void onStart() throws Exception {
        super.onStart()
        realmEnergy = createRealm("energy", "Energy Test", true)
    }
}
