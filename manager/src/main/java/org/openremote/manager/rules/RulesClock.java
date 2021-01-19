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

import org.openremote.container.timer.TimerService;
import org.openremote.model.rules.TemporaryFact;
import org.openremote.model.util.TimeUtil;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.function.Predicate;

/**
 * A fact that is present in any {@link RulesFacts}.
 */
public class RulesClock {

    final public long timestamp;
    final public LocalDateTime time;

    public RulesClock(TimerService timerService) {
        this(timerService.getCurrentTimeMillis());
    }

    public RulesClock(long timestamp) {
        this.timestamp = timestamp;
        time = LocalDateTime.ofInstant(Instant.ofEpochMilli(this.timestamp), ZoneId.systemDefault());
    }

    public long getTimestamp() {
        return timestamp;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public ZonedDateTime getZonedTime() {
        return time.atZone(ZoneId.systemDefault());
    }

    public Instant getInstant() {
        return getZonedTime().toInstant();
    }

    public <T extends TemporaryFact> Predicate<T> last(String timeWindow) {
        double timeWindowStart = getTimestamp() - TimeUtil.parseTimeDuration(timeWindow);
        return fact -> timeWindowStart <= fact.getTimestamp();
    }

    public DayOfWeek getDayOfWeek() {
        return getTime().getDayOfWeek();
    }

    public LocalDateTime getYesterday() {
        return getTime().minus(1, ChronoUnit.DAYS);
    }

    public LocalDateTime getTomorrow() {
        return getTime().plus(1, ChronoUnit.DAYS);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "timestamp=" + timestamp +
            ", time=" + time +
            '}';
    }
}
