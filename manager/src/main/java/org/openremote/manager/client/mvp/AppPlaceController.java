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