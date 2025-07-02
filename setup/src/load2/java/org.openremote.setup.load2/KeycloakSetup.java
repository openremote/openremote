/*
 * Copyright 2025, OpenRemote Inc.
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
package org.openremote.setup.load2;

import org.openremote.manager.setup.AbstractKeycloakSetup;
import org.openremote.model.Container;
import org.openremote.model.security.Realm;
import org.openremote.model.security.User;

import java.util.concurrent.ExecutorService;

public class KeycloakSetup extends AbstractKeycloakSetup {

    public Realm realmOne;
    public User realmOneUser;
    public Realm realmTwo;
    public User realmTwoUser;

    public KeycloakSetup(Container container, ExecutorService executor) {
        super(container);
    }

    @Override
    public void onStart() throws Exception {
        super.onStart();

        realmOne = createRealm("realmone", "Realm One", true);
        realmOneUser = createUser(realmOne.getName(), "user1", "user1", "User", "One", "user1@openremote.local", true, REGULAR_USER_ROLES);

        realmTwo = createRealm("realmtwo", "Realm Two", true);
        realmTwoUser = createUser(realmTwo.getName(), "user1", "user1", "User", "One", "user1@openremote.local", true, REGULAR_USER_ROLES);
    }
}
