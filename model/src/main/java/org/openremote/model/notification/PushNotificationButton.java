/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.model.notification;

import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Values;

/**
 * Represents an action button that can be shown on push alerts (dependent on console support)
 */
public class PushNotificationButton {

    protected String title;
    protected PushNotificationAction action;

    public PushNotificationButton(String title, PushNotificationAction action) {
        this.title = title;
        this.action = action;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public PushNotificationAction getAction() {
        return action;
    }

    public void setAction(PushNotificationAction action) {
        this.action = action;
    }

    public ObjectValue toValue() {
        ObjectValue val = Values.createObject();
        val.put("title", title);
        if (action != null) {
            val.put("action", action.toValue());
        }

        return val;
    }

    public static PushNotificationButton fromValue(ObjectValue value) {
        if (value == null) {
            return null;
        }

        return new PushNotificationButton(
            value.getString("title").orElse(null),
            value.getObject("action").map(PushNotificationAction::fromValue).orElse(null)
        );
    }
}
