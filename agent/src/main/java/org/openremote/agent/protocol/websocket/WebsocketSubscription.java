/*
 * Copyright 2019, OpenRemote Inc.
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
package org.openremote.agent.protocol.websocket;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;

@JsonSubTypes({
    @JsonSubTypes.Type(value = WebsocketSubscriptionImpl.class, name = WebsocketSubscriptionImpl.TYPE),
    @JsonSubTypes.Type(value = WebsocketHTTPSubscription.class, name = WebsocketHTTPSubscription.TYPE)
})
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "type",
    defaultImpl = WebsocketSubscriptionImpl.class
)
public abstract class WebsocketSubscription implements Serializable {

    public Object body;

    public WebsocketSubscription body(Object body) {
        this.body = body;
        return this;
    }
}
