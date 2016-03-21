package org.openremote.manager.client.event;

import org.openremote.manager.shared.event.Event;

public class UserChangeEvent extends Event {

    final private String username;

    public UserChangeEvent(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
