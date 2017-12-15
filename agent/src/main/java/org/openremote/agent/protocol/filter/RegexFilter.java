/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.agent.protocol.filter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.openremote.model.value.StringValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static org.openremote.agent.protocol.filter.RegexFilter.NAME;

@JsonTypeName(NAME)
public class RegexFilter extends MessageFilter<StringValue> {

    public static final String NAME = "regex";

    @JsonProperty
    protected Pattern pattern;
    @JsonProperty
    protected int matchGroup;
    @JsonProperty
    protected int matchIndex;

    @JsonCreator
    public RegexFilter(@JsonProperty("pattern") String regex,
                       @JsonProperty("matchGroup") int matchGroup,
                       @JsonProperty("matchIndex") int matchIndex) {
        try {
            pattern = Pattern.compile(regex);
            this.matchGroup = matchGroup;
            this.matchIndex = matchIndex;
        } catch (PatternSyntaxException ignore) {}
    }

    public RegexFilter(Pattern pattern, int matchGroup, int matchIndex) {
        this.pattern = pattern;
        this.matchGroup = matchGroup;
        this.matchIndex = matchIndex;
    }

    @Override
    public Class<StringValue> getMessageType() {
        return StringValue.class;
    }

    @Override
    public Value process(StringValue value) {
        if (value == null || pattern == null) {
            return null;
        }

        String filteredStr = null;
        Matcher matcher = pattern.matcher(value.getString());
        int matchIndex = 0;
        boolean matched = matcher.find();

        while(matched && matchIndex<this.matchIndex) {
            matched = matcher.find();
            matchIndex++;
        }

        if (matched) {
            if (matchGroup <= matcher.groupCount()) {
                filteredStr = matcher.group(matchGroup);
            }
        }

        return filteredStr == null ? null : Values.create(filteredStr);
    }
}
