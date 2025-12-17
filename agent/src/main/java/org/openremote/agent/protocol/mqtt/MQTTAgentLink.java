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

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.openremote.model.asset.agent.AgentLink;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class MQTTAgentLink extends AgentLink<MQTTAgentLink> {
  @JsonPropertyDescription(
      "An MQTT topic to subscribe to, any received payload will be pushed into the attribute; use value filter(s) to extract values from string payloads and/or value converters to do simple value mapping, complex processing may require a rule or a custom MQTT agent")
  protected String subscriptionTopic;

  @JsonPropertyDescription(
      "An MQTT topic to publish attribute events to, any received payload will be pushed into the attribute; use write value converter and/or write value to do any processing, complex processing may require a rule or a custom MQTT agent")
  protected String publishTopic;

  @JsonPropertyDescription("QoS level to use for publish/subscribe (default is 0 if unset)")
  @Max(2) @Min(0) protected Integer qos;

  @JsonSerialize
  protected String getType() {
    return getClass().getSimpleName();
  }

  // For Hydrators
  protected MQTTAgentLink() {}

  public MQTTAgentLink(String id) {
    super(id);
  }

  public Optional<String> getSubscriptionTopic() {
    return Optional.ofNullable(subscriptionTopic);
  }

  public MQTTAgentLink setSubscriptionTopic(String subscriptionTopic) {
    this.subscriptionTopic = subscriptionTopic;
    return this;
  }

  public Optional<String> getPublishTopic() {
    return Optional.ofNullable(publishTopic);
  }

  public MQTTAgentLink setPublishTopic(String publishTopic) {
    this.publishTopic = publishTopic;
    return this;
  }

  public Optional<Integer> getQos() {
    return Optional.ofNullable(qos);
  }

  public MQTTAgentLink setQos(Integer qos) {
    this.qos = qos;
    return this;
  }
}
