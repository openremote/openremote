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
package org.openremote.model;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Optional;

/**
 * Unified naming of value holder validation failures. {@link Reason} names are supposed to be
 * unique so user-readable strings can be created from names with I18N mapping. The parameter can be
 * used to convey more specific failure information (is dependent on I18N implementation) e.g. Missing
 * meta item 'my.custom.meta.item'.
 */
public class ValidationFailure {

    @JsonSerialize(as = Reason.class)
    @JsonDeserialize(as = ReasonImpl.class)
    public interface Reason {
        @JsonProperty("name")
        String name();
    }

    public static class ReasonImpl implements Reason {
        @JsonProperty("name")
        protected final String name;

        @JsonCreator
        public ReasonImpl(@JsonProperty("name") String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }
    }

    public Reason reason;
    public String parameter;

    protected ValidationFailure() {
    }

    public ValidationFailure(Reason reason) {
        this(reason, null);
    }

    public ValidationFailure(Reason reason, String parameter) {
        this.reason = reason;
        this.parameter = parameter;
    }

    public Reason getReason() {
        return reason;
    }

    public Optional<String> getParameter() {
        return Optional.ofNullable(parameter);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "reason=" + reason +
            ", param='" + parameter + '\'' +
            '}';
    }
}
