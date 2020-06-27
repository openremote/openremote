/*
 * Copyright 2020, OpenRemote Inc.
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
package org.openremote.model.gateway;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openremote.model.event.shared.SharedEvent;

import java.util.Date;

/**
 * Used to indicate to a connected gateway should should disconnect
 * with the {@link #reason} indicating why the disconnect has been
 * requested.
 */
public class GatewayDisconnectEvent extends SharedEvent {

    public enum Reason {
        TERMINATING,
        DISABLED,
        ALREADY_CONNECTED,
        UNRECOGNISED,
        PERMANENT_ERROR
    }

    protected Reason reason;

    @JsonCreator
    public GatewayDisconnectEvent(@JsonProperty("timestamp") Date timestamp, @JsonProperty("reason") Reason reason) {
        super(timestamp != null ? timestamp.getTime() : new Date().getTime());
        this.reason = reason;
    }

    public GatewayDisconnectEvent(Reason reason) {
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
