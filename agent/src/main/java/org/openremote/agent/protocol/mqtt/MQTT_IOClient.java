/*
 * Copyright 2021, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.agent.protocol.mqtt;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.openremote.model.auth.UsernamePassword;

public class MQTT_IOClient extends AbstractMQTT_IOClient<String> {

  public MQTT_IOClient(
      String clientId,
      String host,
      int port,
      boolean secure,
      boolean cleanSession,
      UsernamePassword usernamePassword,
      URI websocketURI,
      MQTTLastWill lastWill,
      KeyManagerFactory keyManagerFactory,
      TrustManagerFactory trustManagerFactory) {
    super(
        clientId,
        host,
        port,
        secure,
        cleanSession,
        usernamePassword,
        websocketURI,
        lastWill,
        keyManagerFactory,
        trustManagerFactory);
  }

  public MQTT_IOClient(
      String clientId,
      String host,
      int port,
      boolean secure,
      boolean cleanSession,
      UsernamePassword usernamePassword,
      URI websocketURI,
      MQTTLastWill lastWill) {
    super(
        clientId,
        host,
        port,
        secure,
        cleanSession,
        usernamePassword,
        websocketURI,
        lastWill,
        null,
        null);
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
