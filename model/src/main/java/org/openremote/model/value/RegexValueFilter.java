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
package org.openremote.model.value;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaTitle;
import io.swagger.v3.oas.annotations.media.Schema;
import org.openremote.model.util.ValueUtil;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@JsonSchemaTitle("Regex")
@JsonTypeName(RegexValueFilter.NAME)
public class RegexValueFilter extends ValueFilter {

    public static final String NAME = "regex";

    @JsonSerialize(using = ToStringSerializer.class)
    public Pattern pattern;
    public Integer matchGroup;
    public Integer matchIndex;

    protected RegexValueFilter() {}

    public RegexValueFilter(Pattern pattern) {
        this.pattern = pattern;
    }

    @JsonCreator
    public RegexValueFilter(@JsonProperty("pattern") String pattern,
                            @JsonProperty("dotAll") Boolean dotAll,
                            @JsonProperty("multiline") Boolean multiline,
                            @JsonProperty("matchGroup") int matchGroup,
                            @JsonProperty("matchIndex") int matchIndex) {
        this(pattern, dotAll == null || dotAll, multiline != null && multiline);
        this.matchGroup = matchGroup;
        this.matchIndex = matchIndex;
    }

    public RegexValueFilter(String pattern, boolean dotAll, boolean multiline) {
        this(Pattern.compile(pattern, (dotAll ? Pattern.DOTALL : 0) | (multiline ? Pattern.MULTILINE : 0)));
    }

    public RegexValueFilter setMatchGroup(Integer matchGroup) {
        this.matchGroup = matchGroup;
        return this;
    }

    public RegexValueFilter setMatchIndex(Integer matchIndex) {
        this.matchIndex = matchIndex;
        return this;
    }

    @Override
    public Object filter(Object value) {
        if (pattern == null) {
            return null;
        }

        Optional<String> valueStr = ValueUtil.getValue(value, String.class, true);
        if (!valueStr.isPresent()) {
            return null;
        }

        String filteredStr = null;
        Matcher matcher = pattern.matcher(valueStr.get());
        int matchIndex = 0;
        boolean matched = matcher.find();

        if (this.matchIndex != null) {
            while (matched && matchIndex < this.matchIndex) {
                matched = matcher.find();
                matchIndex++;
            }
        }

        if (matched) {
            int matchGroup = this.matchGroup == null ? 0 : this.matchGroup;
            if (matchGroup <= matcher.groupCount()) {
                filteredStr = matcher.group(matchGroup);
            }
        }

        return filteredStr;
    }
}
