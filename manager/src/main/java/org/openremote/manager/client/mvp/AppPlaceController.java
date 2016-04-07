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
import com.google.gwt.place.shared.PlaceChangeEvent;
import com.google.gwt.place.shared.PlaceChangeRequestEvent;
import com.google.gwt.place.shared.PlaceController;
import elemental.client.Browser;
import org.openremote.manager.client.event.GoToPlaceEvent;
import org.openremote.manager.client.event.WillGoToPlaceEvent;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.service.SecurityService;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This provides the ability to intercept the place change outside of an activity
 * mapper and to change the place before anything else happens. Also maps place change
 * events from the legacy event bus to our event bus.
 */
public class AppPlaceController extends PlaceController {

    private static final Logger LOG = Logger.getLogger(AppPlaceController.class.getName());

    private SecurityService securityService;

    public AppPlaceController(SecurityService securityService,
                              EventBus eventBus,
                              com.google.web.bindery.event.shared.EventBus legacyEventBus) {
        super(legacyEventBus);
        this.securityService = securityService;

        legacyEventBus.addHandler(PlaceChangeEvent.TYPE, event -> {
            if (LOG.isLoggable(Level.FINE))
                LOG.fine("Received place change, go to: " + event.getNewPlace());
            eventBus.dispatch(new GoToPlaceEvent(event.getNewPlace()));
        });

        legacyEventBus.addHandler(PlaceChangeRequestEvent.TYPE, event -> {
            if (LOG.isLoggable(Level.FINE))
                LOG.fine("Received place change request, will go to: " + event.getNewPlace());
            eventBus.dispatch(new WillGoToPlaceEvent(event.getNewPlace()) {
                @Override
                public String getWarning() {
                    return event.getWarning();
                }

                @Override
                public void setWarning(String warning) {
                    event.setWarning(warning);
                }
            });
        });
    }

    @Override
    public void goTo(Place newPlace) {
        // TODO Should we also update access token before every API call?
        securityService.updateToken(
            SecurityService.MIN_VALIDITY_SECONDS,
            refreshed -> {
                // If it wasn't refreshed, it was still valid, in both cases we can continue
                super.goTo(newPlace);
            },
            () -> {
                // TODO Better error handling
                Browser.getWindow().alert("Error refreshing access token. Sorry.");
            }
        );
    }
}