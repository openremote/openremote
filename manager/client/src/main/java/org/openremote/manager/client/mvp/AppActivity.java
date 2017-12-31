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
package org.openremote.manager.client.mvp;

import com.google.gwt.place.shared.Place;
import org.openremote.manager.client.service.SecurityService;
import org.openremote.model.Constants;
import org.openremote.model.event.bus.EventBus;
import org.openremote.model.event.bus.EventRegistration;

import java.util.Collection;

/**
 * Our own activity class because we don't want their event bus crap.
 */
public abstract class AppActivity<P extends Place>  {

    protected P place;

    public String mayStop() {
        return null;
    }

    public void onCancel() {
    }

    public void onStop() {
    }

    public AppActivity<P> init(SecurityService securityService, P place) throws RoleRequiredException {
        for (String requiredRole : getRequiredRoles()) {
            if (!securityService.hasResourceRoleOrIsSuperUser(requiredRole, Constants.KEYCLOAK_CLIENT_ID)) {
                throw new RoleRequiredException(requiredRole);
            }
        }
        this.place = place;
        return init(place);
    }

    public P getPlace() {
        return place;
    }

    protected abstract AppActivity<P> init(P place);

    /**
     * Any registrations added to the supplied collection will be unregistered automatically when the activity stops.
     */
    abstract public void start(AcceptsView container, EventBus eventBus, Collection<EventRegistration> registrations);

    /**
     * Checked when the activity is initialized, return empty array to allow all access.
     */
    protected String[] getRequiredRoles() {
        return new String[0];
    }
}
