/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.ccs;

import org.apache.log4j.Logger;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.security.AuthForm;
import rx.Observable;

import static org.openremote.ccs.Constants.*;
import static rx.Observable.fromCallable;

public class DemoDataService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(DemoDataService.class.getName());

    public static final String IMPORT_DEMO_DATA = "IMPORT_DEMO_DATA";
    public static final boolean IMPORT_DEMO_DATA_DEFAULT = false;

    public static final String ADMIN_CLI_CLIENT_ID = "admin-cli";
    public static final String ADMIN_PASSWORD = "admin";

    protected Container container;
    protected PersistenceService persistenceService;
    protected CCSIdentityService identityService;

    @Override
    public void init(Container container) throws Exception {
        this.container = container;
        persistenceService = container.getService(PersistenceService.class);
        identityService = container.getService(CCSIdentityService.class);
    }

    @Override
    public void configure(Container container) throws Exception {

    }

    @Override
    public void start(Container container) {
        if (!container.isDevMode() && !container.getConfigBoolean(IMPORT_DEMO_DATA, IMPORT_DEMO_DATA_DEFAULT)) {
            return;
        }

        LOG.info("--- IMPORTING DEMO DATA ---");

        // Use a non-proxy client to get the access token
        String accessToken = identityService.getKeycloak().getAccessToken(
            MASTER_REALM, new AuthForm(ADMIN_CLI_CLIENT_ID, MASTER_REALM_ADMIN_USER, ADMIN_PASSWORD)
        ).getToken();

        try {
            configureMasterRealm(accessToken);

            LOG.info("--- DEMO DATA IMPORT COMPLETE ---");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void stop(Container container) {
    }

    protected void configureMasterRealm(String accessToken) {
        RealmResource realmResource = identityService.getRealms(accessToken, false).realm(MASTER_REALM);
        ClientsResource clientsResource = identityService.getRealms(accessToken, false).realm(MASTER_REALM).clients();
        UsersResource usersResource = identityService.getRealms(accessToken, false).realm(MASTER_REALM).users();

        RealmRepresentation masterRealm = realmResource.toRepresentation();

        masterRealm.setDisplayName("Master");

        identityService.configureRealm(masterRealm, 300);

        realmResource.update(masterRealm);

        // Find out if there is a client already present for this application, if so, delete it
        fromCallable(clientsResource::findAll)
            .flatMap(Observable::from)
            .filter(clientRepresentation -> clientRepresentation.getClientId().equals(APP_CLIENT_ID))
            .map(ClientRepresentation::getId)
            .subscribe(clientObjectId -> {
                clientsResource.get(clientObjectId).remove();
            });

        identityService.createClientApplication(accessToken, masterRealm.getRealm());

        String clientObjectId = fromCallable(() -> clientsResource.findByClientId(APP_CLIENT_ID))
            .flatMap(Observable::from)
            .map(ClientRepresentation::getId)
            .toBlocking().singleOrDefault(null);

        ClientResource clientResource = clientsResource.get(clientObjectId);
        RolesResource rolesResource = clientResource.roles();

        // TODO User and role mappings
    }

}
