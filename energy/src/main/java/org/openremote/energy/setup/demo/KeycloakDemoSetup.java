package org.openremote.energy.setup.demo;

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
        keycloakProvider.updateUserRoles(realmCity.getName(), smartCityUserId, "account"); // Remove all roles for account client
        User manufacturerUser = createUser(realmManufacturer.getName(), "manufacturer", "manufacturer", "Agri", "Tech", null, true, demoUserRoles);
        manufacturerUserId = manufacturerUser.getId();
        keycloakProvider.updateUserRoles(realmManufacturer.getName(), manufacturerUserId, "account"); // Remove all roles for account client
        User customerUser = createUser(realmManufacturer.getName(), "customer", "customer", "Bert", "Frederiks", null, true, demoUserRoles);
        customerUserId = customerUser.getId();
        keycloakProvider.updateUserRoles(realmManufacturer.getName(), customerUserId, "account"); // Remove all roles for account client
    }
}

