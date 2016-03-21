package org.openremote.manager.client.mvp;

import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventRegistration;

import java.util.Collection;

/**
 * Our own activity class because we don't want their event bus crap.
 */
public abstract class AppActivity<T extends Place>  {

    public AppActivity doInit(T place) {
        init(place);
        return this;
    }

    public String mayStop() {
        return null;
    }

    public void onCancel() {
    }

    public void onStop() {
    }

    abstract protected void init(T place);

    /**
     * Any registrations added to the supplied collection will be unregistered automatically when the activity stops.
     */
    abstract public void start(AcceptsOneWidget container, EventBus eventBus, Collection<EventRegistration> registrations);
}
