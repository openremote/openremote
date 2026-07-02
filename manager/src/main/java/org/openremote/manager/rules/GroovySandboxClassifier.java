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

import java.util.Set;

public class GroovySandboxClassifier {

    protected static final Set<String> DANGEROUS_MEMBERS = Set.of(
        "classLoader",
        "getClass",
        "getClassLoader",
        "forName",
        "metaClass",
        "setMetaClass",
        "invokeMethod"
    );
    protected static final String CLASS_LITERAL_MEMBER = "class";

    protected static final Set<String> DANGEROUS_TYPES = Set.of(
        "java.io.File",
        "java.lang.Class",
        "java.lang.ClassLoader",
        "java.lang.Process",
        "java.lang.ProcessBuilder",
        "java.lang.Runtime",
        "java.lang.SecurityManager",
        "java.lang.System",
        "java.lang.Thread",
        "java.lang.ThreadGroup",
        "java.lang.reflect.AccessibleObject",
        "java.lang.reflect.Constructor",
        "java.lang.reflect.Field",
        "java.lang.reflect.Method",
        "java.net.ServerSocket",
        "java.net.Socket",
        "java.net.URI",
        "java.net.URL",
        "java.nio.file.Files",
        "java.nio.file.Path",
        "java.nio.file.Paths",
        "org.codehaus.groovy.runtime.InvokerHelper"
    );

    protected static final Set<String> KNOWN_TYPES = Set.of(
        "groovy.lang.Binding",
        "groovy.lang.Closure",
        "groovy.lang.Script",
        "groovy.lang.Tuple",
        "groovy.transform.ToString",
        "java.lang.Boolean",
        "java.lang.Byte",
        "java.lang.Character",
        "java.lang.Double",
        "java.lang.Exception",
        "java.lang.Float",
        "java.lang.Integer",
        "java.lang.Long",
        "java.lang.Math",
        "java.lang.Number",
        "java.lang.Object",
        "java.lang.RuntimeException",
        "java.lang.Short",
        "java.lang.String",
        "java.lang.Throwable",
        "java.math.BigDecimal",
        "java.math.BigInteger",
        "java.time.Duration",
        "java.time.Instant",
        "java.time.LocalDate",
        "java.time.LocalDateTime",
        "java.time.LocalTime",
        "java.time.ZoneId",
        "java.time.ZonedDateTime",
        "java.util.ArrayDeque",
        "java.util.ArrayList",
        "java.util.Arrays",
        "java.util.Collection",
        "java.util.Collections",
        "java.util.Comparator",
        "java.util.Date",
        "java.util.HashMap",
        "java.util.HashSet",
        "java.util.LinkedHashMap",
        "java.util.LinkedHashSet",
        "java.util.List",
        "java.util.Map",
        "java.util.Objects",
        "java.util.Optional",
        "java.util.Set",
        "java.util.UUID",
        "java.util.logging.Logger",
        "java.util.stream.Stream",
        "org.openremote.manager.rules.RulesBuilder",
        "org.openremote.manager.rules.RulesBuilder$Builder",
        "org.openremote.manager.rules.RulesFacts"
    );

    protected GroovySandboxClassifier() {
    }

    public static GroovySandboxClassification classify(
        GroovySandboxOperation operation,
        String receiverType,
        String member
    ) {
        if (isDangerous(operation, receiverType, member)) {
            return GroovySandboxClassification.DANGEROUS;
        }

        if (isKnownClassLiteral(receiverType, member)) {
            return GroovySandboxClassification.KNOWN;
        }

        if (isKnown(receiverType)) {
            return GroovySandboxClassification.KNOWN;
        }

        return GroovySandboxClassification.UNKNOWN;
    }

    protected static boolean isDangerous(GroovySandboxOperation operation, String receiverType, String member) {
        if (operation == GroovySandboxOperation.SOURCE_GRAB) {
            return true;
        }

        if (receiverType != null && DANGEROUS_TYPES.contains(receiverType)) {
            return true;
        }

        if (receiverType != null
            && (receiverType.startsWith("java.lang.reflect.")
                || receiverType.startsWith("java.net.")
                || receiverType.startsWith("java.nio.file.")
                || receiverType.startsWith("javax.script.")
                || receiverType.startsWith("groovy.grape.")
                || receiverType.startsWith("org.codehaus.groovy.runtime.ProcessGroovyMethods"))) {
            return true;
        }

        return member != null && DANGEROUS_MEMBERS.contains(member);
    }

    protected static boolean isKnownClassLiteral(String receiverType, String member) {
        return CLASS_LITERAL_MEMBER.equals(member) && isKnown(receiverType);
    }

    protected static boolean isKnown(String receiverType) {
        if (receiverType == null) {
            return false;
        }

        return KNOWN_TYPES.contains(receiverType)
            || receiverType.startsWith("java.util.")
            || receiverType.startsWith("java.time.")
            || receiverType.startsWith("org.openremote.model.")
            || receiverType.startsWith("org.openremote.manager.rules.facade.");
    }
}
