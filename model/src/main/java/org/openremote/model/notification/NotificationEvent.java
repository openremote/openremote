package org.openremote.model.notification;

import org.openremote.model.PersistenceEvent;
import org.openremote.model.event.shared.RealmScopedEvent;

public class NotificationEvent extends RealmScopedEvent {
    protected String realm;
    protected PersistenceEvent.Cause cause;

    public NotificationEvent(String realm, PersistenceEvent.Cause cause) {
        this.realm = realm;
        this.cause = cause;
    }

    @Override
    public String getRealm() {
        return realm;
    }

    public PersistenceEvent.Cause getCause() {
        return cause;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + 
        "realm=" + realm + 
        ", cause=" + cause + 
        "}";
    }
}
