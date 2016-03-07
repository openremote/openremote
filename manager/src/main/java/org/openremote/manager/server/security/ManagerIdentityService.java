package org.openremote.manager.server.security;

import org.openremote.container.security.IdentityService;
import org.openremote.container.web.WebService;
import org.openremote.manager.server.Constants;
import org.openremote.manager.server.web.ManagerWebService;

public class ManagerIdentityService extends IdentityService {

    @Override
    protected boolean enableKeycloakReverseProxy() {
        return true;
    }

    @Override
    protected Class<? extends WebService> getWebServiceClass() {
        return ManagerWebService.class;
    }

    @Override
    protected String getClientId() {
        return Constants.MANAGER_CLIENT_ID;
    }
}
