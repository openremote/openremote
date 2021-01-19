/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.model.event;

import org.openremote.model.util.TextUtil;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

/**
 * A timestamped event.
 * <p>
 * The event type string is a lowercase, dash-separated name of the event class without the "event" suffix. For
 * example, the type string of <code>AssetTreeModifiedEvent</code> is <code>asset-tree-modified</code>. The event
 * type is therefore usable in JavaScript frameworks, e.g. when declaring Polymer event listeners.
 */
@MappedSuperclass
public abstract class Event {

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "TIMESTAMP", updatable = false, nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    public Date timestamp;

    protected Event(long timestamp) {
        this.timestamp = new Date(timestamp);
    }

    protected Event() {
    }

    public static String getEventType(String simpleClassName) {
        String type = TextUtil.toLowerCaseDash(simpleClassName);
        if (type.length() > 6 && type.endsWith("-event"))
            type = type.substring(0, type.length() - 6);
        return type;
    }

    public static String getEventType(Class<? extends Event> eventClass) {
        return getEventType(eventClass.getSimpleName());
    }

    final public String getEventType() {
        return getEventType(getClass());
    }

    public long getTimestamp() {
        return timestamp != null ? timestamp.getTime() : 0L;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = new Date(timestamp);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "timestamp=" + timestamp +
            '}';
    }
}
