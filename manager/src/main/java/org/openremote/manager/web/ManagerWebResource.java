/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.manager.web;

import org.openremote.container.timer.TimerService;
import org.openremote.container.web.WebResource;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.model.security.Realm;

public class ManagerWebResource extends WebResource {

    final protected TimerService timerService;
    final protected ManagerIdentityService identityService;

    public ManagerWebResource(TimerService timerService, ManagerIdentityService identityService) {
        this.timerService = timerService;
        this.identityService = identityService;
    }

    public boolean isRestrictedUser() {
        return isAuthenticated() && identityService.getIdentityProvider().isRestrictedUser(getAuthContext());
    }

    public Realm getAuthenticatedRealm() {
        return identityService.getIdentityProvider().getRealm(getAuthenticatedRealmName());
    }

    public Realm getRequestRealm() {
        return identityService.getIdentityProvider().getRealm(getRequestRealmName());
    }

    public boolean isRealmActiveAndAccessible(String realm) {
        return identityService.getIdentityProvider().isRealmActiveAndAccessible(this, realm);
    }

    public boolean isRealmActiveAndAccessible(Realm realm) {
        return identityService.getIdentityProvider().isRealmActiveAndAccessible(this, realm);
    }

}
