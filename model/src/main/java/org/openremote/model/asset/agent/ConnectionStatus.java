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

import org.openremote.model.attribute.MetaItem;

/**
 * Indicates the status of a protocol configuration (i.e. protocol instance); but can also be used internally by a
 * protocol if desired.
 */
public enum ConnectionStatus {

    /**
     * To be used when it is not possible to determine the status.
     */
    UNKNOWN,

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

    /**
     * Disabled.
     * <p>
     * For protocol configurations this generally means that it is disabled (i.e. it has a
     * {@link org.openremote.model.asset.AssetMeta#DISABLED} {@link MetaItem} with a value of <code>true</code>).
     */
    DISABLED,

    /**
     * An operation is being performed that means the status cannot be exactly determined at this time (e.g. the
     * trying to re-establish a connection to a remote server).
     */
    WAITING,

    /**
     * Connection is closed and cannot be re-used.
     */
    CLOSED,

    /**
     * An authentication related error has occurred.
     */
    ERROR_AUTHENTICATION,

    /**
     * A configuration related error has occurred.
     */
    ERROR_CONFIGURATION,

    /**
     * A general error has occurred that prevents normal operation.
     */
    ERROR
}
