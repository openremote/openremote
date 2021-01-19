/*
 * Copyright 2021, OpenRemote Inc.
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
package org.openremote.model.value.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import net.fortuna.ical4j.model.Dur;
import org.openremote.model.util.TextUtil;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Duration;
import java.time.Period;

/**
 * Allows proper ISO8601 duration values which combine {@link java.time.Duration} and {@link java.time.Period}
 */
public class PeriodAndDuration implements Serializable {
    @JsonIgnore
    protected Period period;
    @JsonIgnore
    protected Duration duration;

    @JsonCreator
    protected PeriodAndDuration(@NotNull String str) {
        if (TextUtil.isNullOrEmpty(str)) {
            throw new IllegalArgumentException("Value cannot be null or empty");
        }

        String[] periodAndDuration = str.split("T");
        if (periodAndDuration.length == 2 && periodAndDuration[1].length() > 0) {
            duration = Duration.parse("P" + periodAndDuration[1]);
        }
        if (periodAndDuration[0].length() > 1) {
            period = Period.parse(periodAndDuration[0]);
        }
    }

    public PeriodAndDuration(Period period, Duration duration) {
        this.period = period;
        this.duration = duration;
    }

    public Period getPeriod() {
        return period;
    }

    public Duration getDuration() {
        return duration;
    }

    @Override
    public String toString() {
        return (period != null ? period.toString() : "P") + (duration != null ? duration.toString().substring(1) : "");
    }
}
