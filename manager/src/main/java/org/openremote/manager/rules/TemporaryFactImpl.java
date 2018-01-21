/*
 * Copyright 2018, OpenRemote Inc.
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

import javaemul.internal.annotations.GwtIncompatible;
import org.openremote.model.event.Event;
import org.openremote.model.rules.TemporaryFact;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * An arbitrary value as a punctual event in rules processing.
 */
@GwtIncompatible
public class TemporaryFactImpl<T> extends Event implements TemporaryFact {

    final protected long expirationMilliseconds;
    final protected T fact;
    // These are only useful for debugging, easier to read than timestamps
    final public LocalDateTime time;
    final public LocalDateTime expirationTime;

    public TemporaryFactImpl(long timestamp, long expirationMilliseconds, T fact) {
        super(timestamp);
        this.expirationMilliseconds = expirationMilliseconds;
        this.fact = fact;
        this.time = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        this.expirationTime = time.plus(expirationMilliseconds, ChronoUnit.MILLIS);
    }

    @Override
    public long getExpirationMilliseconds() {
        return expirationMilliseconds;
    }

    public T getFact() {
        return fact;
    }

    @Override
    public String toString() {
        return TemporaryFact.class.getSimpleName() + "{" +
            "timestamp=" + timestamp +
            ", time=" + time +
            ", expirationMilliseconds=" + expirationMilliseconds +
            ", expirationTime=" + expirationTime +
            ", fact=" + fact +
            '}';
    }
}
