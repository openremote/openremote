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

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.openremote.model.alarm.Alarm;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Optional;

import static org.openremote.model.value.ForecastConfigurationWeightedExponentialAverage.TYPE;


@JsonTypeInfo(property = "type", use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
        @JsonSubTypes.Type(AnomalyDetectionConfiguration.Global.class),
        @JsonSubTypes.Type(AnomalyDetectionConfiguration.Change.class),
        @JsonSubTypes.Type(AnomalyDetectionConfiguration.Timespan.class),
        @JsonSubTypes.Type(AnomalyDetectionConfiguration.Forecast.class)
})
public abstract class AnomalyDetectionConfiguration implements Serializable {

    public String name;
    public Boolean onOff;
    public Integer deviation;
    public Alarm alarm;



    public AnomalyDetectionConfiguration(@JsonProperty("name") String name, @JsonProperty("onOff") Boolean onOff, @JsonProperty("deviation") Integer deviation, @JsonProperty("alarm") Alarm alarm){
        this.name = name;
        this.onOff = onOff;
        this.deviation = deviation;
        this.alarm = alarm;
    }

    public boolean isValid() {
        return deviation != null;
    }


    @JsonTypeName("global")
    public static class Global extends AnomalyDetectionConfiguration {

        public Integer minimumDatapoints;
        @JsonSerialize(using = ToStringSerializer.class)
        @JsonDeserialize(converter = ForecastConfigurationWeightedExponentialAverage.PeriodAndDurationConverter.class)
        public ForecastConfigurationWeightedExponentialAverage.ExtendedPeriodAndDuration timespan;

        @JsonCreator
        public Global(@JsonProperty("name") String name, @JsonProperty("onOff") Boolean onOff, @JsonProperty("deviation") Integer deviation, @JsonProperty("alarm") Alarm alarm, @JsonProperty("minimumDatapoints") Integer minimumDatapoints, @JsonProperty("timespan") ForecastConfigurationWeightedExponentialAverage.ExtendedPeriodAndDuration timespan) {
            super(name,onOff, deviation,alarm);
            this.minimumDatapoints = minimumDatapoints;
            this.timespan = timespan;
        }

    }

    @JsonTypeName("change")
    public static class Change extends AnomalyDetectionConfiguration {
        public Integer minimumDatapoints;
        @JsonSerialize(using = ToStringSerializer.class)
        @JsonDeserialize(converter = ForecastConfigurationWeightedExponentialAverage.PeriodAndDurationConverter.class)
        public ForecastConfigurationWeightedExponentialAverage.ExtendedPeriodAndDuration timespan;

        @JsonCreator
        public Change(@JsonProperty("name") String name, @JsonProperty("onOff") Boolean onOff, @JsonProperty("deviation") Integer deviation, @JsonProperty("alarm") Alarm alarm, @JsonProperty("minimumDatapoints") Integer minimumDatapoints, @JsonProperty("timespan") ForecastConfigurationWeightedExponentialAverage.ExtendedPeriodAndDuration timespan) {
            super(name,onOff, deviation,alarm);
            this.minimumDatapoints = minimumDatapoints;
            this.timespan = timespan;
        }
    }

    @JsonTypeName("timespan")
    public static class Timespan extends AnomalyDetectionConfiguration {
        public Integer minimumDatapoints;
        @JsonSerialize(using = ToStringSerializer.class)
        @JsonDeserialize(converter = ForecastConfigurationWeightedExponentialAverage.PeriodAndDurationConverter.class)
        public ForecastConfigurationWeightedExponentialAverage.ExtendedPeriodAndDuration timespan;

        @JsonCreator
        public Timespan(@JsonProperty("name") String name, @JsonProperty("onOff") Boolean onOff, @JsonProperty("deviation") Integer deviation, @JsonProperty("alarm") Alarm alarm, @JsonProperty("minimumDatapoints") Integer minimumDatapoints, @JsonProperty("timespan") ForecastConfigurationWeightedExponentialAverage.ExtendedPeriodAndDuration timespan) {
            super(name,onOff, deviation,alarm);
            this.minimumDatapoints = minimumDatapoints;
            this.timespan = timespan;
        }
    }

    @JsonTypeName("forecast")
    public static class Forecast extends AnomalyDetectionConfiguration {

        @JsonCreator
        public Forecast(@JsonProperty("name") String name, @JsonProperty("onOff") Boolean onOff, @JsonProperty("deviation") Integer deviation, @JsonProperty("alarm") Alarm alarm) {
            super(name,onOff, deviation,alarm);
        }
    }
}
