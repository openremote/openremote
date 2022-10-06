/*
 * Copyright 2022, OpenRemote Inc.
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
package org.openremote.manager.mqtt;

import org.apache.activemq.artemis.core.config.WildcardConfiguration;
import org.apache.activemq.artemis.core.protocol.mqtt.MQTTUtil;
import org.openremote.model.util.TextUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Topic {

    public static final String SEPARATOR = "/";
    public static final String SINGLE_LEVEL_TOKEN = "+";
    public static final String MULTI_LEVEL_TOKEN = "#";
    public static final Topic EMPTY_TOPIC = new Topic(SEPARATOR, Collections.emptyList());

    protected String topic;
    protected List<String> tokens;

    public static Topic fromAddress(String address, WildcardConfiguration wildcardConfiguration) throws IllegalArgumentException {
        return Topic.parse(MQTTUtil.convertCoreAddressToMqttTopicFilter(address, wildcardConfiguration));
    }

    public static Topic parse(String topic) throws IllegalArgumentException {
        if (TextUtil.isNullOrEmpty(topic) || SEPARATOR.equals(topic)) {
            return EMPTY_TOPIC;
        }

        int multiLevelPos = topic.indexOf(MULTI_LEVEL_TOKEN);
        if (multiLevelPos >= 0 && multiLevelPos < topic.length() - 1) {
            throw new IllegalArgumentException("Multilevel wildcard token must be at the end of the topic");
        }

        List<String> tokens = Arrays.asList(topic.split(SEPARATOR));
        return new Topic(topic, tokens);
    }

    protected Topic(String topic, List<String> tokens) {
        this.topic = topic;
        this.tokens = tokens;
    }

    public String getString() {
        return topic;
    }

    public boolean hasWildcard() {
        return topic.contains(SINGLE_LEVEL_TOKEN) || topic.contains(MULTI_LEVEL_TOKEN);
    }

    public List<String> getTokens() {
        return tokens;
    }

    @Override
    public String toString() {
        return topic;
    }
}
