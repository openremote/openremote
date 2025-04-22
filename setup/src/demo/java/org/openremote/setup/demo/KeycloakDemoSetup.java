/*
 * Copyright 2020, OpenRemote Inc.
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
package org.openremote.setup.demo;

import org.openremote.manager.setup.AbstractKeycloakSetup;
import org.openremote.model.Constants;
import org.openremote.model.Container;
import org.openremote.model.security.ClientRole;
import org.openremote.model.security.Realm;
import org.openremote.model.security.User;

import java.util.Arrays;
import java.util.logging.Logger;

/**
 * We have the following demo users:
 * <ul>
 * <li><code>admin</code> - The superuser in the "master" realm with all access</li>
 * <li><code>smartcity</code> - (Password: smartcity) A user in the "smartcity" realm with read access</li>
 * <li><code>manufacturer</code> - (Password: manufacturer) A user in the "manufacturer" realm with read access</li>
 * <li><code>manufacturer - customer</code> - (Password: customer) A user in the "manufacturer" realm with restricted access to his assets</li>
 *
 * </ul>
 */
public class KeycloakDemoSetup extends AbstractKeycloakSetup {

    private static final Logger LOG = Logger.getLogger(KeycloakDemoSetup.class.getName());

    public String smartCityUserId;
    public static String manufacturerUserId;
    public static String customerUserId;
    public Realm realmMaster;
    public Realm realmCity;
    public static Realm realmManufacturer;

    public KeycloakDemoSetup(Container container) {
        super(container);
    }

    @Override
    public void onStart() throws Exception {
        super.onStart();

        // Realms
        realmMaster = identityService.getIdentityProvider().getRealm(Constants.MASTER_REALM);
        realmCity = createRealm("smartcity", "Smart City", true);
        realmManufacturer = createRealm("manufacturer", "Manufacturer", true);
        removeManageAccount("smartcity");
        removeManageAccount("manufacturer");

        // Don't allow demo users to write assets
        ClientRole[] demoUserRoles = Arrays.stream(AbstractKeycloakSetup.REGULAR_USER_ROLES)
            .filter(clientRole -> clientRole != ClientRole.WRITE_ASSETS)
            .toArray(ClientRole[]::new);

        // Users
        User smartCityUser = createUser(realmCity.getName(), "smartcity", "smartcity", "Smart", "City", null, true, demoUserRoles);
        this.smartCityUserId = smartCityUser.getId();
        keycloakProvider.updateUserClientRoles(realmCity.getName(), smartCityUserId, "account"); // Remove all roles for account client
        User manufacturerUser = createUser(realmManufacturer.getName(), "manufacturer", "manufacturer", "Agri", "Tech", null, true, demoUserRoles);
        manufacturerUserId = manufacturerUser.getId();
        keycloakProvider.updateUserClientRoles(realmManufacturer.getName(), manufacturerUserId, "account"); // Remove all roles for account client
        User customerUser = createUser(realmManufacturer.getName(), "customer", "customer", "Bert", "Frederiks", null, true, demoUserRoles);
        customerUserId = customerUser.getId();
        keycloakProvider.updateUserClientRoles(realmManufacturer.getName(), customerUserId, "account"); // Remove all roles for account client
    }
}
