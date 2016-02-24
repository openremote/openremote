package org.openremote.manager.client.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.place.shared.Place;

/**
 * Event used to indicate that a login is required
 * Created by Richard on 24/02/2016.
 */
public class LoginRequestEvent extends GwtEvent<LoginRequestEvent.Handler> {
    public static final Type<Handler> TYPE = new Type<Handler>();
    private Place redirectPlace;

    public LoginRequestEvent(Place redirectPlace) {
        this.redirectPlace = redirectPlace;
    }

    public Place getRedirectPlace() {
        return redirectPlace;
    }

    @Override
    public Type<Handler> getAssociatedType() {
        return TYPE;
    }

    @Override
    public void dispatch(Handler handler) {
        handler.onRequestLogin(this);
    }

    public interface Handler extends EventHandler {
        void onRequestLogin(LoginRequestEvent event);
    }
}
