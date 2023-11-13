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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Optional;

import static org.openremote.model.value.ForecastConfigurationWeightedExponentialAverage.TYPE;


@JsonTypeInfo(property = "type", use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
        @JsonSubTypes.Type(AnomalyDetectionConfiguration.Global.class),
        @JsonSubTypes.Type(AnomalyDetectionConfiguration.Change.class),
        @JsonSubTypes.Type(AnomalyDetectionConfiguration.Timespan.class),
})
public abstract class AnomalyDetectionConfiguration implements Serializable {

    public Boolean onOff;
    public Integer deviation;
    public Integer minimumDatapoints;
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(converter = ForecastConfigurationWeightedExponentialAverage.PeriodAndDurationConverter.class)
    public ForecastConfigurationWeightedExponentialAverage.ExtendedPeriodAndDuration timespan;


    public AnomalyDetectionConfiguration(@JsonProperty("onOff") Boolean onOff, @JsonProperty("deviation") Integer deviation, @JsonProperty("minimumDatapoints") Integer minimumDatapoints, @JsonProperty("timespan") ForecastConfigurationWeightedExponentialAverage.ExtendedPeriodAndDuration timespan){
        this.onOff = onOff;
        this.deviation = deviation;
        this.minimumDatapoints = minimumDatapoints;
        this.timespan = timespan;
    }

    public boolean isValid() {
        return deviation != null && deviation > 0 && deviation < 201 &&
                minimumDatapoints != null && minimumDatapoints > 1 &&
                timespan != null && timespan.toMillis() > 0;
    }


    @JsonTypeName("global")
    public static class Global extends AnomalyDetectionConfiguration {

        @JsonCreator
        public Global(@JsonProperty("onOff") Boolean onOff, @JsonProperty("deviation") Integer deviation, @JsonProperty("minimumDatapoints") Integer minimumDatapoints, @JsonProperty("timespan") ForecastConfigurationWeightedExponentialAverage.ExtendedPeriodAndDuration timespan) {
            super(onOff, deviation,minimumDatapoints, timespan);
        }

    }

    @JsonTypeName("change")
    public static class Change extends AnomalyDetectionConfiguration {

        @JsonCreator
        public Change(@JsonProperty("onOff") Boolean onOff, @JsonProperty("deviation") Integer deviation, @JsonProperty("minimumDatapoints") Integer minimumDatapoints, @JsonProperty("timespan") ForecastConfigurationWeightedExponentialAverage.ExtendedPeriodAndDuration timespan) {
            super(onOff, deviation,minimumDatapoints,timespan);
        }
    }

    @JsonTypeName("timespan")
    public static class Timespan extends AnomalyDetectionConfiguration {

        @JsonCreator
        public Timespan(@JsonProperty("onOff") Boolean onOff, @JsonProperty("deviation") Integer deviation, @JsonProperty("minimumDatapoints") Integer minimumDatapoints, @JsonProperty("timespan") ForecastConfigurationWeightedExponentialAverage.ExtendedPeriodAndDuration timespan) {
            super(onOff, deviation,minimumDatapoints,timespan);
        }
    }
}
