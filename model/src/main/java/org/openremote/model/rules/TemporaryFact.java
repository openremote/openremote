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
package org.openremote.model.rules;

import org.openremote.model.event.Event;
import org.openremote.model.util.TsIgnore;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * A rule fact that has a timestamp, it expires after a certain time.
 */
@TsIgnore
public class TemporaryFact<T> extends Event {

    final protected long expirationMilliseconds;
    final protected T fact;
    // These are only useful for debugging, easier to read than timestamps
    final public LocalDateTime time;
    final public LocalDateTime expirationTime;

    public TemporaryFact(long timestamp, long expirationMilliseconds, T fact) {
        super(timestamp);
        this.expirationMilliseconds = expirationMilliseconds;
        this.fact = fact;
        this.time = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        this.expirationTime = time.plus(expirationMilliseconds, ChronoUnit.MILLIS);
    }

    public long getExpirationMilliseconds() {
        return expirationMilliseconds;
    }

    public T getFact() {
        return fact;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public LocalDateTime getExpirationTime() {
        return expirationTime;
    }

    public boolean isExpired(long currentTimestamp) {
        return getTimestamp() + getExpirationMilliseconds() < currentTimestamp;
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
