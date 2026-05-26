/*
 * Copyright 2023, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.model.value;

import static org.openremote.model.value.ForecastConfigurationWeightedExponentialAverage.TYPE;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import org.openremote.model.util.JSONSchemaUtil.*;
import org.openremote.model.util.TimeUtil;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@JsonTypeName(TYPE)
public class ForecastConfigurationWeightedExponentialAverage extends ForecastConfiguration {

  public static final String TYPE = "wea";

  @NotNull @JsonSerialize(using = ToStringSerializer.class)
  @JsonDeserialize(converter = TimeUtil.PeriodAndDurationConverter.class)
  @JsonSchemaTypeRemap(type = String.class)
  // TODO: consider @JsonSchemaFormat("duration") requires new or-mwc-input type
  protected TimeUtil.ExtendedPeriodAndDuration pastPeriod;

  @NotNull @Positive protected Integer pastCount;

  @NotNull @JsonSerialize(using = ToStringSerializer.class)
  @JsonDeserialize(converter = TimeUtil.PeriodAndDurationConverter.class)
  @JsonSchemaTypeRemap(type = String.class)
  // TODO: consider @JsonSchemaFormat("duration") requires new or-mwc-input type
  protected TimeUtil.ExtendedPeriodAndDuration forecastPeriod;

  @NotNull @Positive protected Integer forecastCount;

  @JsonCreator
  public ForecastConfigurationWeightedExponentialAverage(
      @JsonProperty("pastPeriod") TimeUtil.ExtendedPeriodAndDuration pastPeriod,
      @JsonProperty("pastCount") Integer pastCount,
      @JsonProperty("forecastPeriod") TimeUtil.ExtendedPeriodAndDuration forecastPeriod,
      @JsonProperty("forecastCount") Integer forecastCount) {
    super(TYPE);
    this.pastPeriod = pastPeriod;
    this.pastCount = pastCount;
    this.forecastPeriod = forecastPeriod;
    this.forecastCount = forecastCount;
  }

  @Override
  public boolean isValid() {
    return pastCount != null
        && pastCount > 0
        && forecastCount != null
        && forecastCount > 0
        && pastPeriod != null
        && pastPeriod.toMillis() > 0
        && forecastPeriod != null
        && forecastPeriod.toMillis() > 0;
  }

  public TimeUtil.ExtendedPeriodAndDuration getPastPeriod() {
    return pastPeriod;
  }

  public Integer getPastCount() {
    return pastCount;
  }

  public TimeUtil.ExtendedPeriodAndDuration getForecastPeriod() {
    return forecastPeriod;
  }

  public Integer getForecastCount() {
    return forecastCount;
  }
}
