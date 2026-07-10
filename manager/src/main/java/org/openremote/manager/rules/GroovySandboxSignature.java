/*
 * Copyright 2026, OpenRemote Inc.
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
package org.openremote.manager.rules;

import groovy.lang.Closure;
import groovy.lang.Script;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public record GroovySandboxSignature(
    GroovySandboxPhase phase,
    GroovySandboxOperation operation,
    String receiverType,
    String member,
    List<String> argumentTypes,
    GroovySandboxClassification classification
) {

    public static final String NONE = "-";
    public static final String NULL = "null";

    public GroovySandboxSignature {
        phase = Objects.requireNonNull(phase, "phase");
        operation = Objects.requireNonNull(operation, "operation");
        receiverType = normalize(receiverType);
        member = normalize(member);
        argumentTypes = argumentTypes == null
            ? List.of()
            : List.copyOf(argumentTypes.stream().map(GroovySandboxSignature::normalize).toList());
        classification = Objects.requireNonNullElse(classification, GroovySandboxClassification.UNKNOWN);
    }

    public static GroovySandboxSignature of(
        GroovySandboxPhase phase,
        GroovySandboxOperation operation,
        String receiverType,
        String member,
        GroovySandboxClassification classification,
        String... argumentTypes
    ) {
        return new GroovySandboxSignature(
            phase,
            operation,
            receiverType,
            member,
            argumentTypes == null ? List.of() : Arrays.asList(argumentTypes),
            classification
        );
    }

    public static List<String> typeNames(Object... args) {
        if (args == null || args.length == 0) {
            return List.of();
        }

        return Arrays.stream(args)
            .map(GroovySandboxSignature::typeName)
            .toList();
    }

    public static String typeName(Object value) {
        if (value == null) {
            return NULL;
        }
        if (value instanceof Class<?> type) {
            return type.getName();
        }
        if (value instanceof Script) {
            return Script.class.getName();
        }
        if (value instanceof Closure) {
            return Closure.class.getName();
        }
        return value.getClass().getName();
    }

    protected static String normalize(String value) {
        return value == null || value.isBlank() ? NONE : value.trim();
    }
}
