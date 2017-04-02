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
package org.openremote.model.event.shared;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.openremote.model.AttributeEvent;
import org.openremote.model.asset.AssetModifiedEvent;
import org.openremote.model.event.Event;

/**
 * An event that can be serialized and shared between client and server.
 */
@JsonSubTypes({
    // Events used on client and server (serializable)
    @JsonSubTypes.Type(value = AttributeEvent.class, name = "attribute"),
    @JsonSubTypes.Type(value = AssetModifiedEvent.class, name = "asset-modified")
})
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "eventType"
)
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    creatorVisibility= JsonAutoDetect.Visibility.NONE,
    getterVisibility= JsonAutoDetect.Visibility.NONE,
    setterVisibility= JsonAutoDetect.Visibility.NONE,
    isGetterVisibility= JsonAutoDetect.Visibility.NONE
)
public abstract class SharedEvent extends Event {

    public static final String MESSAGE_PREFIX = "EVENT";

    public SharedEvent(long timestamp) {
        super(timestamp);
    }

    public SharedEvent() {
    }
}
