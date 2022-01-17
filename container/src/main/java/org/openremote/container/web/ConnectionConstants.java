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
package org.openremote.container.web;

public interface ConnectionConstants {

    String SESSION_TERMINATOR = "connection.sessionTerminator";
    String SESSION_KEY = "connection.sessionKey";
    String SEND_TO_ALL = "connection.sendToAll";
    String HANDSHAKE_REALM = "connection.realm";
    String HANDSHAKE_AUTH = "connection.auth";
    String SESSION_OPEN = "connection.sessionOpen";
    String SESSION_CLOSE = "connection.sessionClose";
    String SESSION_CLOSE_ERROR = "connection.sessionCloseError";

}
