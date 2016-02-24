package org.openremote.manager.client.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Event to indicate that the logged in user has changed
 * Created by Richard on 24/02/2016.
 */
public class UserChangeEvent extends GwtEvent<UserChangeEvent.Handler> {
    public static final Type<Handler> TYPE = new Type<Handler>();
    private String username;

    public UserChangeEvent(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public Type<Handler> getAssociatedType() {
        return TYPE;
    }

    @Override
    public void dispatch(Handler handler) {
        handler.onUserChange(this);
    }

    public interface Handler extends EventHandler {
        void onUserChange(UserChangeEvent event);
    }
}
