/*
 * Copyright 2017, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.manager.web;

import org.openremote.container.timer.TimerService;
import org.openremote.container.web.WebResource;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.model.security.Realm;

public class ManagerWebResource extends WebResource {

  protected final TimerService timerService;
  protected final ManagerIdentityService identityService;

  public ManagerWebResource(TimerService timerService, ManagerIdentityService identityService) {
    this.timerService = timerService;
    this.identityService = identityService;
  }

  public boolean isRestrictedUser() {
    return isAuthenticated()
        && identityService.getIdentityProvider().isRestrictedUser(getAuthContext());
  }

  public Realm getAuthenticatedRealm() {
    return identityService.getIdentityProvider().getRealm(getAuthenticatedRealmName());
  }

  public Realm getRequestRealm() {
    return identityService.getIdentityProvider().getRealm(getRequestRealmName());
  }

  public boolean isRealmActiveAndAccessible(String realm) {
    return identityService
        .getIdentityProvider()
        .isRealmActiveAndAccessible(getAuthContext(), realm);
  }

  public boolean isRealmActiveAndAccessible(Realm realm) {
    return identityService
        .getIdentityProvider()
        .isRealmActiveAndAccessible(getAuthContext(), realm);
  }
}
