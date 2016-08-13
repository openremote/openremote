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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.openremote.manager.shared.ngsi.simplequery.Query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@JsonSerialize(using = Condition.ConditionSerialiser.class)
public class Condition {
    public static class ConditionSerialiser extends StdSerializer<Condition> {
        public ConditionSerialiser() {
            this(null);
        }

        public ConditionSerialiser(Class<Condition> t) {
            super(t);
        }

        @Override
        public void serialize(Condition value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            if (value.query != null) {
                gen.writeObjectFieldStart("expression");
                gen.writeObject(value.query);
                gen.writeEndObject();
            }
            gen.writeArrayFieldStart("attrs");
            for(String attr : value.attrs) {
                gen.writeString(attr);
            }
            gen.writeEndArray();
            gen.writeEndObject();
        }
    }

    // Unfortunately Jackson doesn't honour the always include for empty list (bug in Jackson https://github.com/FasterXML/jackson-databind/issues/1327)
    @JsonInclude(JsonInclude.Include.ALWAYS)
    protected List<String> attrs;
    @JsonProperty("expression")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected Query query;

    public Condition() {
        attrs = new ArrayList<>();
    }

    public Condition(List<String> attributes) {
        this.attrs = attributes;
    }

    public Condition(Query query) {
        this.query = query;
    }

    public Condition(List<String> attributes, Query query) {
        this.attrs = attributes;
        this.query = query;
    }

    public List<String> getAttributes() {
        return attrs;
    }

    public Query getQuery() {
        return query;
    }


//    protected String[] getAttributesArr() { return attrs == null ? new String[0] : attrs.toArray(new String[0]); }
//
//    @JsonProperty("attrs")
//    protected void setAttributesArr(String[] attrs) {
//        this.attrs = Arrays.asList(attrs);
//    }
}
