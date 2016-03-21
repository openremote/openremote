package org.openremote.manager.client.event;

import com.google.gwt.place.shared.Place;
import org.openremote.manager.shared.event.Event;

public class GoToPlaceEvent extends Event {

    private final Place newPlace;

    public GoToPlaceEvent(Place newPlace) {
        this.newPlace = newPlace;
    }

    public Place getNewPlace() {
        return newPlace;
    }
}
