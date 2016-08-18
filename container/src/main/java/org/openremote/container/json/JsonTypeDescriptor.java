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
package org.openremote.container.json;

import elemental.json.JsonValue;
import elemental.json.impl.JsonUtil;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.hibernate.type.descriptor.java.MutableMutabilityPlan;

public class JsonTypeDescriptor extends AbstractTypeDescriptor<JsonValue> {

    public JsonTypeDescriptor() {
        super(JsonValue.class, new MutableMutabilityPlan<JsonValue>() {
            @Override
            protected JsonValue deepCopyNotNull(JsonValue value) {
                return JsonUtil.parse(value.toJson());
            }
        });
    }

    @Override
    public boolean areEqual(JsonValue one, JsonValue another) {
        if (one == another) {
            return true;
        }
        if (one == null || another == null) {
            return false;
        }
        return one.toJson().equals(another.toJson());
    }

    @Override
    public String toString(JsonValue value) {
        return value.toJson();
    }

    @Override
    public JsonValue fromString(String string) {
        return JsonUtil.parse(string);
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public <X> X unwrap(JsonValue value, Class<X> type, WrapperOptions options) {
        if (value == null) {
            return null;
        }
        if (String.class.isAssignableFrom(type)) {
            return (X) toString(value);
        }
        throw unknownUnwrap(type);
    }

    @Override
    public <X> JsonValue wrap(X value, WrapperOptions options) {
        if (value == null) {
            return null;
        }
        return fromString(value.toString());
    }

}