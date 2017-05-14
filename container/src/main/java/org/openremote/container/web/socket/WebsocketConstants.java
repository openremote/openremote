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
package org.openremote.container.web.socket;

public interface WebsocketConstants {

    int SESSION_MAX_IDLE_TIMEOUT_SECONDS = 300;
    String SESSION = "websocket.session";
    String SESSION_KEY = "websocket.sessionKey";
    String SEND_TO_ALL = "websocket.sendToAll";
    String HANDSHAKE_AUTH = "websocket.auth";
    String SESSION_OPEN = "websocket.sessionOpen";
    String SESSION_CLOSE = "websocket.sessionClose";
    String SESSION_CLOSE_ERROR = "websocket.sessionCloseError";

}
