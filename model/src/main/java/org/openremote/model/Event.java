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
package org.openremote.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Locale;

/*
@JsonSubTypes({
    // Events used on client and server (serialized on client/server bus)
    @JsonSubTypes.Type(value = AttributeStateUpdateEvent.class, name = "ATTRIBUTE_STATE_UPDATE")
})
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "event"
)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
*/
// TODO Any subclass that is not @JsonIgnoreType will be made serializable (and must be compilable in GWT!)
// TODO This needs more work when we know how to do client/server pub/sub message bus
// TODO Should sender be compulsory?
/**
 * A timestamped event with optional sender.
 */
public abstract class Event {
    protected Class<?> sender;
    protected long timestamp;

    protected Event(long timestamp, Class<?> sender) {
        this.timestamp = timestamp;
        this.sender = sender;
    }

    protected Event() {
        this(System.currentTimeMillis(), null);
    }

    protected Event(Class<?> sender) {
        this(System.currentTimeMillis(), sender);
    }

    /**
     * Transforms <code>EXFooBar123</code> into <code>ex-foo-bar-123</code> and
     * <code>attributeX</code> into <code>attribute-x</code> without regex (GWT!)
     */
    public static String toLowerCaseDash(String camelCase) {
        if (camelCase == null)
            return null;
        if (camelCase.length() == 0)
            return camelCase;
        StringBuilder sb = new StringBuilder();
        char[] chars = camelCase.toCharArray();
        boolean inNonLowerCase = false;
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (!Character.isLowerCase(c)) {
                if (!inNonLowerCase) {
                    if (i > 0)
                        sb.append("-");
                } else if (i < chars.length - 1 && Character.isLowerCase(chars[i + 1])) {
                    sb.append("-");
                }
                inNonLowerCase = true;
            } else {
                inNonLowerCase = false;
            }
            sb.append(c);
        }
        String name = sb.toString();
        name = name.toLowerCase(Locale.ROOT);
        return name;
    }

    public static String getEventType(String simpleClassName) {
        String type = toLowerCaseDash(simpleClassName);
        if (type.length() > 6 && type.substring(type.length() - 6).equals("-event"))
            type = type.substring(0, type.length() - 6);
        return type;
    }

    public static String getEventType(Class<? extends Event> actionClass) {
        return getEventType(actionClass.getSimpleName());
    }

    /**
     * A type name that is compile with Polymer event naming, for example the subclass
     * <code>FooBarEvent</code> will be <code>foo-bar</code>.
     */
    public String getEventType() {
        return getEventType(getClass());
    }

    @JsonIgnore
    public long getTimestamp() {
        return timestamp;
    }

    @JsonIgnore
    public Class<?> getSender() {
        return sender;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "timestamp=" + timestamp +
            '}';
    }
}
