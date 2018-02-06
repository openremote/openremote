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

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.function.Predicate;

/**
 * A fact that is present in any {@link RulesFacts}.
 */
public class RulesClock {

    final public double timestamp;
    final public LocalDateTime time;

    public RulesClock(TimerService timerService) {
        this(timerService.getCurrentTimeMillis());
    }

    public RulesClock(double timestamp) {
        this.timestamp = timestamp;
        time = LocalDateTime.ofInstant(Instant.ofEpochMilli((long) this.timestamp), ZoneId.systemDefault());
    }

    public double getTimestamp() {
        return timestamp;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public <T extends TemporaryFact> Predicate<T> last(String timeWindow) {
        double timeWindowStart = getTimestamp() - TimeUtil.parseTimeString(timeWindow);
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
