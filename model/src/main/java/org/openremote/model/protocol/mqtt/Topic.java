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
package org.openremote.model.protocol.mqtt;

import jakarta.validation.constraints.NotNull;
import org.openremote.model.util.TextUtil;

import java.util.Objects;

public class Topic {

    public static final String SEPARATOR = "/";
    public static final String SINGLE_LEVEL_TOKEN = "+";
    public static final String MULTI_LEVEL_TOKEN = "#";
    public static final Topic EMPTY_TOPIC = new Topic(SEPARATOR, new String[0]);

    protected String topicStr;
    protected String[] tokens;

    public static Topic parse(String topic) throws IllegalArgumentException {
        if (TextUtil.isNullOrEmpty(topic) || SEPARATOR.equals(topic)) {
            return EMPTY_TOPIC;
        }

        int multiLevelPos = topic.indexOf(MULTI_LEVEL_TOKEN);
        if (multiLevelPos >= 0 && multiLevelPos < topic.length() - 1) {
            throw new IllegalArgumentException("Multilevel wildcard token must be at the end of the topic");
        }

        String[] tokens = topic.split(SEPARATOR);
        return new Topic(topic, tokens);
    }

    protected Topic(String topicStr, String[] tokens) {
        this.topicStr = topicStr;
        this.tokens = tokens;
    }

    public boolean hasWildcard() {
        return topicStr.contains(SINGLE_LEVEL_TOKEN) || topicStr.contains(MULTI_LEVEL_TOKEN);
    }

    public String[] getTokens() {
        return tokens;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Topic topic = (Topic) o;
        return Objects.equals(topicStr, topic.topicStr);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(topicStr);
    }

    @Override
    public String toString() {
        return topicStr;
    }

    /**
     * Determines if the specified topic matches this topic; can be a simple equality match or if this topic contains
     * wildcards then determines if the specified topic is covered by the wildcards
     */
    public boolean matches(String topic) {
        return matches(Topic.parse(topic));
    }

    /**
     * Determines if the specified topic matches this topic; can be a simple equality match or if this topic contains
     * wildcards then determines if the specified topic is covered by the wildcards
     */
    public boolean matches(@NotNull Topic topic) {
        if (topic == null || topic.hasWildcard()) {
            throw new IllegalArgumentException("Topic cannot be null or contain wildcards");
        }

        if (Objects.equals(this.topicStr, topic.topicStr)) {
            return true;
        }

        int i = 0;
        for (; i < tokens.length; i++) {
            String w = tokens[i];

            if (w.equals("#")) {
                return true;
            }

            if (i >= topic.tokens.length) {
                return false;
            }

            if (w.equals("+")) {
                continue;
            }

            if (!w.equals(topic.tokens[i])) {
                return false;
            }
        }

        return i == topic.tokens.length;
    }
}
