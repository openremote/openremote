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

import org.openremote.model.value.ArrayValue;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Value;

import java.util.List;
import java.util.Optional;

/**
 * Holds a {@link Value}.
 */
public interface ValueHolder {

    enum ValueFailureReason implements ValidationFailure.Reason {
        VALUE_PERCENTAGE_OUT_OF_RANGE,
        VALUE_INVALID_COLOR_FORMAT,
        VALUE_TEMPERATURE_OUT_OF_RANGE,
        VALUE_NUMBER_OUT_OF_RANGE,
        VALUE_INVALID,
        VALUE_MISMATCH,
        VALUE_REQUIRED
    }

    void clearValue();

    Optional<Value> getValue();

    Optional<String> getValueAsString();

    Optional<Double> getValueAsNumber();

    Optional<Integer> getValueAsInteger();

    Optional<Boolean> getValueAsBoolean();

    Optional<ObjectValue> getValueAsObject();

    Optional<ArrayValue> getValueAsArray();

    void setValue(Value value);

    List<ValidationFailure> getValidationFailures();
}
