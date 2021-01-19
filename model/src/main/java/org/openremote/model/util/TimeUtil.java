/*
 * Copyright 2010 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openremote.model.util;

import org.openremote.model.Constants;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;

/**
 * A helper class with utility methods for
 * time related operations.
 */
public class TimeUtil {

    // Simple syntax
    protected static final Pattern SIMPLE = Pattern.compile(Constants.ISO8601_DURATION_REGEXP);

    /**
     * Parses the given time duration String and returns the corresponding number of milliseconds.
     *
     * @throws NullPointerException if time is null
     */
    public static long parseTimeDuration(String duration) {
        try {
            if (duration.startsWith("PT")) {
                return Duration.parse(duration).toMillis();
            }
            LocalDateTime start = LocalDateTime.now();
            return start.until(start.plus(Period.parse(duration)), ChronoUnit.MILLIS);
        } catch (DateTimeParseException e) {
            throw new RuntimeException("Error parsing time duration string: [ " + duration + " ]");
        }
    }

    public static boolean isTimeDuration(String time) {
        time = time != null ? time.trim() : null;
        return time != null && time.length() > 0
                && (SIMPLE.matcher(time).matches()
                    || isTimeDurationPositiveInfinity(time)
                    || isTimeDurationNegativeInfinity(time));
    }

    public static boolean isTimeDurationPositiveInfinity(String time) {
        time = time != null ? time.trim() : null;
        return "*".equals(time) || "+*".equals(time);
    }

    public static boolean isTimeDurationNegativeInfinity(String time) {
        time = time != null ? time.trim() : null;
        return "-*".equals(time);
    }

    /**
     * Parses ISO8601 strings with optional time and/or offset; if no zone is provided then UTC is assumed if no
     * time is provided then 00:00:00 is assumed.
     */
    public static long parseTimeIso8601(String datetime) {
        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(ISO_LOCAL_DATE)
            .optionalStart()           // time made optional
            .appendLiteral('T')
            .append(ISO_LOCAL_TIME)
            .optionalStart()           // zone and offset made optional
            .appendOffsetId()
            .optionalStart()
            .appendLiteral('[')
            .parseCaseSensitive()
            .appendZoneRegionId()
            .appendLiteral(']')
            .optionalEnd()
            .optionalEnd()
            .optionalEnd()
            .toFormatter();

        TemporalAccessor temporalAccessor = formatter.parseBest(datetime, ZonedDateTime::from, LocalDateTime::from, LocalDate::from);
        ZonedDateTime zonedDateTime;

        if (temporalAccessor instanceof ZonedDateTime) {
            zonedDateTime = (ZonedDateTime)temporalAccessor;
        } else if (temporalAccessor instanceof LocalDateTime) {
            zonedDateTime = ((LocalDateTime)temporalAccessor).atZone(ZoneOffset.UTC);
        } else {
            zonedDateTime = ((LocalDate) temporalAccessor).atStartOfDay(ZoneOffset.UTC);
        }
        return zonedDateTime.toInstant().toEpochMilli();
    }
}
