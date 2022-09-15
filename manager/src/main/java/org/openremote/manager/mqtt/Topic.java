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

import org.openremote.model.util.TextUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Topic {

    public static final Topic EMPTY_TOPIC = new Topic(".");
    public static final String SINGLE_LEVEL_TOKEN = "+";
    public static final String MULTI_LEVEL_TOKEN = "*";

    protected String topic;
    protected List<String> tokens;

    public static Topic parse(String topic) throws IllegalArgumentException {
        if (TextUtil.isNullOrEmpty(topic) || ".".equals(topic)) {
            return EMPTY_TOPIC;
        }

        int multiLevelPos = topic.indexOf(MULTI_LEVEL_TOKEN);
        if (multiLevelPos == 0 || multiLevelPos < topic.length() - 1) {
            throw new IllegalArgumentException("Multilevel wildcard token must be at the end of the topic");
        }

        return new Topic(topic);
    }

    protected Topic(String topic) {
        this.topic = topic;
        if (".".equals(topic)) {
            tokens = Collections.emptyList();
        } else {
            this.tokens = Arrays.asList(topic.split("\\."));
        }
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
}
