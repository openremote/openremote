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

@JsonSubTypes({
    @JsonSubTypes.Type(value = WebsocketSubscription.class, name = WebsocketSubscription.TYPE),
    @JsonSubTypes.Type(value = WebsocketHttpSubscription.class, name = WebsocketHttpSubscription.TYPE)
})
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "type",
    defaultImpl = WebsocketSubscription.class
)
public class WebsocketSubscription<T> {

    public static final String TYPE = "websocket";

    public String type = TYPE;
    public T body;

    public WebsocketSubscription() {
    }

    protected WebsocketSubscription(String type) {
        this.type = type;
    }

    public WebsocketSubscription<T> body(T body) {
        this.body = body;
        return this;
    }
}
