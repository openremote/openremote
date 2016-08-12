/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.manager.shared.ngsi.params;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openremote.manager.shared.ngsi.simplequery.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Condition {
    @JsonIgnore
    protected List<String> attributes;
    @JsonProperty("expression")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected Query query;

    public Condition() {
        attributes = new ArrayList<>();
    }

    public Condition(List<String> attributes) {
        this.attributes = attributes;
    }

    public Condition(Query query) {
        this.query = query;
    }

    public Condition(List<String> attributes, Query query) {
        this.attributes = attributes;
        this.query = query;
    }

    public List<String> getAttributes() {
        return attributes;
    }

    public Query getQuery() {
        return query;
    }

    @JsonProperty("attrs")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    protected String[] getAttributesArr() { return attributes == null ? new String[0] : attributes.toArray(new String[0]); }

    @JsonProperty("attrs")
    protected void setAttributesArr(String[] attributes) {
        this.attributes = Arrays.asList(attributes);
    }
}
