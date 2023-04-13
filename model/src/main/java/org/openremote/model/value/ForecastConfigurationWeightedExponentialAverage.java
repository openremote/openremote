/*
 * Copyright 2023, OpenRemote Inc.
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
package org.openremote.model.value;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.databind.util.StdConverter;
import org.openremote.model.Constants;
import org.openremote.model.value.impl.PeriodAndDuration;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.regex.Pattern;

import static org.openremote.model.value.ForecastConfigurationWeightedExponentialAverage.TYPE;

@JsonTypeName(TYPE)
public class ForecastConfigurationWeightedExponentialAverage extends ForecastConfiguration {

    public static final String TYPE = "wea";

    protected static final Pattern iso8601Pattern = Pattern.compile(Constants.ISO8601_DURATION_REGEXP);

    @NotNull
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(converter = PeriodAndDurationConverter.class)
    protected ForecastConfigurationWeightedExponentialAverage.ExtendedPeriodAndDuration pastPeriod;
    @NotNull
    @Positive
    protected Integer pastCount;
    @NotNull
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(converter = PeriodAndDurationConverter.class)
    protected ForecastConfigurationWeightedExponentialAverage.ExtendedPeriodAndDuration forecastPeriod;
    @NotNull
    @Positive
    protected Integer forecastCount;

    @JsonCreator
    public ForecastConfigurationWeightedExponentialAverage(@JsonProperty("pastPeriod") ExtendedPeriodAndDuration pastPeriod, @JsonProperty("pastCount") Integer pastCount, @JsonProperty("forecastPeriod") ExtendedPeriodAndDuration forecastPeriod, @JsonProperty("forecastCount") Integer forecastCount) {
        super(TYPE);
        this.pastPeriod = pastPeriod;
        this.pastCount = pastCount;
        this.forecastPeriod = forecastPeriod;
        this.forecastCount = forecastCount;
    }

    @Override
    public boolean isValid() {
        return pastCount != null && pastCount > 0 &&
               forecastCount != null && forecastCount > 0 &&
               pastPeriod != null && pastPeriod.toMillis() > 0 &&
               forecastPeriod != null && forecastPeriod.toMillis() > 0;
    }

    public ExtendedPeriodAndDuration getPastPeriod() {
        return pastPeriod;
    }

    public Integer getPastCount() {
        return pastCount;
    }

    public ExtendedPeriodAndDuration getForecastPeriod() {
        return forecastPeriod;
    }

    public Integer getForecastCount() {
        return forecastCount;
    }

    public static class ExtendedPeriodAndDuration extends PeriodAndDuration {
        public ExtendedPeriodAndDuration(String str) {
            super(str);
            if (!iso8601Pattern.matcher(str).matches()) {
                throw new IllegalArgumentException("Invalid ISO 8601 format");
            }
        }

        public long toMillis() {
            Period period = (getPeriod() != null ? getPeriod() : Period.ZERO);
            Duration duration = (getDuration() != null ? getDuration() : Duration.ZERO);
            return duration.plus(durationFromPeriod(period)).toMillis();
        }

        private Duration durationFromPeriod(Period period) {
            Duration years = ChronoUnit.YEARS.getDuration().multipliedBy(period.getYears());
            Duration months = ChronoUnit.MONTHS.getDuration().multipliedBy(period.getMonths());
            Duration days = ChronoUnit.DAYS.getDuration().multipliedBy(period.getDays());
            return years.plus(months).plus(days);
        }
    }

    public static class PeriodAndDurationConverter extends StdConverter<JsonNode, ExtendedPeriodAndDuration> {
        @Override
        public ExtendedPeriodAndDuration convert(JsonNode value) {
            if (value.isTextual()) {
                return new ExtendedPeriodAndDuration(value.asText());
            }
            return null;
        }
    }
}
