package org.openremote.ccs;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.openremote.container.Container;

import static org.openremote.ccs.Constants.APP_CLIENT_ID;
import static org.openremote.ccs.Constants.APP_NAME;

public class CCSIdentityService extends org.openremote.container.security.IdentityService {

    protected Container container;

    @Override
    public void init(Container container) throws Exception {
        this.container = container;
        setClientId(APP_CLIENT_ID);
        super.init(container);
    }

    public void createClientApplication(String bearerAuth, String realm) {
        ClientsResource clientsResource = getRealms(bearerAuth, false).realm(realm).clients();
        ClientRepresentation client = createClientApplication(
            realm, getClientId(), APP_NAME, container.isDevMode()
        );
        clientsResource.create(client);
        client = clientsResource.findByClientId(client.getClientId()).get(0);
        ClientResource clientResource = clientsResource.get(client.getId());
        addDefaultRoles(clientResource.roles());
    }

    public void addDefaultRoles(RolesResource rolesResource) {
        rolesResource.create(new RoleRepresentation("account-owner", "Account owner", false));
        rolesResource.create(new RoleRepresentation("service-admin", "Service administrator", false));
    }

}
