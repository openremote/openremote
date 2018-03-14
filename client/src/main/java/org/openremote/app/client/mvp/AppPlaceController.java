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
package org.openremote.app.client.mvp;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceChangeEvent;
import com.google.gwt.place.shared.PlaceChangeRequestEvent;
import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.google.web.bindery.event.shared.UmbrellaException;
import org.openremote.app.client.event.GoToPlaceEvent;
import org.openremote.app.client.event.WillGoToPlaceEvent;
import org.openremote.app.client.map.MapAssetPlace;
import org.openremote.model.event.bus.EventBus;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This provides the ability to intercept the place change outside of an activity
 * mapper and to change the place before anything else happens. Also maps place change
 * events from the legacy event bus to our event bus.
 */
public class AppPlaceController extends PlaceController {

    private static final Logger LOG = Logger.getLogger(AppPlaceController.class.getName());

    protected final com.google.web.bindery.event.shared.EventBus legacyEventBus;

    protected HandlerRegistration placeChangeRegistration;
    protected HandlerRegistration placeChangeRequestRegistration;

    public AppPlaceController(EventBus eventBus,
                              com.google.web.bindery.event.shared.EventBus legacyEventBus,
                              Delegate delegate) {
        super(legacyEventBus, delegate);
        this.legacyEventBus = legacyEventBus;

        placeChangeRegistration = legacyEventBus.addHandler(PlaceChangeEvent.TYPE, event -> {
            if (LOG.isLoggable(Level.FINE))
                LOG.fine("Received place change, go to: " + event.getNewPlace());
            eventBus.dispatch(new GoToPlaceEvent(event.getNewPlace()));
        });

        placeChangeRequestRegistration = legacyEventBus.addHandler(PlaceChangeRequestEvent.TYPE, event -> {
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

                @Override
                public String toString() {
                    return "WillGoToPlaceEvent{" +
                        "place=" + event.getNewPlace()+
                        '}';
                }
            });
        });
    }

    @Override
    public void goTo(Place newPlace) {
        try {

            // TODO Evil hack to get rid of the empty # in URL added by Firefox on each reload of start page
            // TODO Still, firefox breaks with keycloak on every third reload and goes back to start page (wrong redirect with #state fragment)
            if (newPlace == Place.NOWHERE) { // TODO Shouldn't really use that place anywhere, have a default place
                super.goTo(new MapAssetPlace()); // This is the start page of the app
                return;
            }

            super.goTo(newPlace);
        } catch (UmbrellaException ex) {
            LOG.log(Level.SEVERE, "Error handling place change to: " + newPlace, ex);
        }
    }

    public void stop() {
        placeChangeRegistration.removeHandler();
        placeChangeRequestRegistration.removeHandler();
    }

    public com.google.web.bindery.event.shared.EventBus getLegacyEventBus() {
        return legacyEventBus;
    }
}