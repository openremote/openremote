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

public class MQTTMessage<T> {

  protected String topic;
  protected T payload;
  protected Integer qos;

  public MQTTMessage(String topic, T payload) {
    this.topic = topic;
    this.payload = payload;
  }

  public MQTTMessage(String topic, T payload, Integer qos) {
    this.topic = topic;
    this.payload = payload;
    this.qos = qos;
  }

  public String getTopic() {
    return topic;
  }

  public T getPayload() {
    return payload;
  }

  public Integer getQos() {
    return qos;
  }

  @Override
  public String toString() {
    return "MQTTMessage{"
        + "topic='"
        + topic
        + '\''
        + ", payload="
        + payload
        + ", qos="
        + qos
        + '}';
  }
}
