package org.openremote.model.alarm;

import org.openremote.model.PersistenceEvent;
import org.openremote.model.event.shared.RealmScopedEvent;

public class AlarmEvent extends RealmScopedEvent {
    protected String realm;
    protected PersistenceEvent.Cause cause;

    public AlarmEvent(String realm, PersistenceEvent.Cause cause) {
        this.realm = realm;
        this.cause = cause;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "realm=" + realm +
                ", cause=" + cause +
                '}';
    }
}
