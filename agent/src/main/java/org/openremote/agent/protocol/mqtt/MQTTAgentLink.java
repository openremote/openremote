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

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.openremote.model.asset.agent.AgentLink;

import java.util.Optional;

public class MQTTAgentLink extends AgentLink<MQTTAgentLink> {

    protected String subscriptionTopic;
    protected String publishTopic;

    @JsonSerialize
    protected String getType() {
        return getClass().getSimpleName();
    }

    // For Hydrators
    protected MQTTAgentLink() {}

    protected MQTTAgentLink(String id) {
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
}
