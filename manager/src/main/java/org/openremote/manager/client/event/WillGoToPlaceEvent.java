package org.openremote.manager.client.event;

import com.google.gwt.place.shared.Place;
import org.openremote.manager.shared.event.Event;

public abstract class WillGoToPlaceEvent extends Event {

    final protected Place newPlace;

    public WillGoToPlaceEvent(Place newPlace) {
        this.newPlace = newPlace;
    }

    public Place getNewPlace() {
        return newPlace;
    }

    abstract public String getWarning();

    abstract public void setWarning(String warning);
}
