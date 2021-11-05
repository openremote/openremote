/*
 * Copyright 2021, OpenRemote Inc.
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
package org.openremote.agent.protocol.mqtt;

import org.openremote.model.auth.UsernamePassword;

import java.net.URI;
import java.nio.charset.StandardCharsets;

public class MQTT_IOClient extends AbstractMQTT_IOClient<String> {

    public MQTT_IOClient(String host, int port, boolean secure, boolean cleanSession, UsernamePassword usernamePassword, URI websocketURI) {
        super(host, port, secure, cleanSession, usernamePassword, websocketURI);
    }

    public MQTT_IOClient(String clientId, String host, int port, boolean secure, boolean cleanSession, UsernamePassword usernamePassword, URI websocketURI) {
        super(clientId, host, port, secure, cleanSession, usernamePassword, websocketURI);
    }

    @Override
    public byte[] messageToBytes(String message) {
        return message != null ? message.getBytes(StandardCharsets.UTF_8) : new byte[0];
    }

    @Override
    public String messageFromBytes(byte[] bytes) {
        return new String(bytes);
    }
}
