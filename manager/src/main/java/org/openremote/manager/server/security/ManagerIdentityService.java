package org.openremote.manager.server.security;

import org.openremote.container.Container;
import org.openremote.container.security.IdentityService;
import org.openremote.manager.server.Constants;

public class ManagerIdentityService extends IdentityService {

    @Override
    public void init(Container container) throws Exception {
        setClientId(Constants.MANAGER_CLIENT_ID);
        super.init(container);
        setKeycloakReverseProxy(true);
    }
}
