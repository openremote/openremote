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
package org.openremote.model.query.filter;

import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDescription;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.NameHolder;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Predicate for string values; will match based on configured options.
 */
@JsonSchemaDescription("Predicate for string values; will match based on configured options.")
public class StringPredicate extends ValuePredicate {

    public static final String name = "string";
    public AssetQuery.Match match = AssetQuery.Match.EXACT;
    public boolean caseSensitive = true;
    public String value;
    public boolean negate;

    public StringPredicate() {
    }

    public StringPredicate(String value) {
        this.value = value;
    }

    public StringPredicate(NameHolder nameProvider) {
        this.value = nameProvider.getName();
    }

    public StringPredicate(AssetQuery.Match match, String value) {
        this.match = match;
        this.value = value;
    }

    public StringPredicate(AssetQuery.Match match, boolean caseSensitive, String value) {
        this.match = match;
        this.caseSensitive = caseSensitive;
        this.value = value;
    }

    public Predicate<Object> asPredicate(Supplier<Long> currentMillisSupplier) {
        return obj -> {

            String string = ValueUtil.getValueCoerced(obj, String.class).orElse(null);

            if (string == null && value == null)
                return !negate;
            if (string == null)
                return negate;
            if (value == null)
                return negate;

            String shouldMatch = caseSensitive ? value : value.toUpperCase(Locale.ROOT);
            String have = caseSensitive ? string : string.toUpperCase(Locale.ROOT);

            switch (match) {
                case BEGIN:
                    return negate != have.startsWith(shouldMatch);
                case END:
                    return negate != have.endsWith(shouldMatch);
                case CONTAINS:
                    return negate != have.contains(shouldMatch);
            }
            return negate != have.equals(shouldMatch);
        };
    }

    public StringPredicate match(AssetQuery.Match match) {
        this.match = match;
        return this;
    }

    public StringPredicate caseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
        return this;
    }

    public StringPredicate value(String value) {
        this.value = value;
        return this;
    }

    public StringPredicate negate(boolean negate) {
        this.negate = negate;
        return this;
    }

    public String prepareValue() {
        String s = match.prepare(this.value);
        if (!caseSensitive)
            s = s.toUpperCase(Locale.ROOT);
        return s;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "match=" + match +
            ", caseSensitive=" + caseSensitive +
            ", negate=" + negate +
            ", value='" + value + '\'' +
            '}';
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(match)
            + Objects.hashCode(caseSensitive)
            + Objects.hashCode(negate)
            + Objects.hashCode(value);
    }

    @Override
    public boolean equals(Object obj) {

        if (!(obj instanceof StringPredicate)) {
            return false;
        }

        StringPredicate that = (StringPredicate)obj;

        return Objects.equals(that.match, match)
            && Objects.equals(that.caseSensitive, caseSensitive)
            && Objects.equals(that.negate, negate)
            && Objects.equals(that.value, value);
    }
}
