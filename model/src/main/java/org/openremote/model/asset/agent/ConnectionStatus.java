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
package org.openremote.model.asset.agent;

/**
 * Indicates the status of an agent's protocol instance; but can also be used internally by a protocol if desired.
 */
public enum ConnectionStatus {

    /**
     * Not connected.
     */
    DISCONNECTED,

    /**
     * Connection in progress.
     */
    CONNECTING,

    /**
     * Disconnection in progress.
     */
    DISCONNECTING,

    /**
     * Connected.
     */
    CONNECTED,

    DISABLED,

    /**
     * An operation is being performed that means the status cannot be exactly determined at this time (e.g. the
     * trying to re-establish a connection to a remote server).
     */
    WAITING,

    /**
     * A general error has occurred that prevents normal operation.
     */
    ERROR,

    STOPPED
}
