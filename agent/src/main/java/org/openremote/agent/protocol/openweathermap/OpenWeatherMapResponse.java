/*
 * Copyright 2025, OpenRemote Inc.
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
package org.openremote.agent.protocol.openweathermap;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * The response object from the OpenWeatherMap API, trimmed down to only include the core weather
 * properties
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenWeatherMapResponse {

  @JsonProperty("current")
  @JsonDeserialize(as = WeatherDatapointImpl.class)
  private WeatherDatapoint current;

  @JsonProperty("hourly")
  @JsonDeserialize(contentAs = WeatherDatapointImpl.class)
  private List<WeatherDatapoint> hourly;

  @JsonProperty("daily")
  @JsonDeserialize(contentAs = DailyWeatherDatapointImpl.class)
  private List<WeatherDatapoint> daily;

  public OpenWeatherMapResponse() {}

  public WeatherDatapoint getCurrent() {
    return current;
  }

  public List<WeatherDatapoint> getHourly() {
    return hourly;
  }

  public List<WeatherDatapoint> getDaily() {
    return daily;
  }

  public interface WeatherDatapoint {
    long getTimestamp();

    int getPressure();

    int getHumidity();

    double getTemperature();

    double getWindSpeed();

    int getWindDegrees();

    double getWindGust();

    int getClouds();

    double getPop();

    double getUvi();

    double getRain();
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class WeatherDatapointImpl implements WeatherDatapoint {

    @JsonProperty("dt")
    private long timestamp;

    @JsonProperty("temp")
    private double temperature;

    @JsonProperty("pressure")
    private int pressure;

    @JsonProperty("humidity")
    private int humidity;

    @JsonProperty("clouds")
    private int clouds;

    @JsonProperty("wind_speed")
    private double windSpeed;

    @JsonProperty("wind_deg")
    private int windDegrees;

    @JsonProperty("wind_gust")
    private double windGust;

    @JsonProperty("pop")
    private double pop;

    @JsonProperty("uvi")
    private double uvi;

    @JsonProperty("rain")
    private Rain rain;

    public WeatherDatapointImpl() {}

    public long getTimestamp() {
      return timestamp;
    }

    public double getTemperature() {
      return temperature;
    }

    public int getPressure() {
      return pressure;
    }

    public int getHumidity() {
      return humidity;
    }

    public int getClouds() {
      return clouds;
    }

    public double getWindSpeed() {
      return windSpeed;
    }

    public int getWindDegrees() {
      return windDegrees;
    }

    public double getWindGust() {
      return windGust;
    }

    public double getPop() {
      return pop;
    }

    public double getUvi() {
      return uvi;
    }

    public double getRain() {
      return rain != null ? rain.getOneHour() : 0.0;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class DailyWeatherDatapointImpl implements WeatherDatapoint {
    @JsonProperty("dt")
    private long timestamp;

    @JsonProperty("temp")
    private Temp temperature;

    @JsonProperty("pressure")
    private int pressure;

    @JsonProperty("humidity")
    private int humidity;

    @JsonProperty("wind_speed")
    private double windSpeed;

    @JsonProperty("wind_deg")
    private int windDegrees;

    @JsonProperty("wind_gust")
    private double windGust;

    @JsonProperty("clouds")
    private int clouds;

    @JsonProperty("pop")
    private double pop;

    @JsonProperty("rain")
    private Double rain;

    @JsonProperty("uvi")
    private double uvi;

    public DailyWeatherDatapointImpl() {}

    public long getTimestamp() {
      return timestamp;
    }

    public int getPressure() {
      return pressure;
    }

    public int getHumidity() {
      return humidity;
    }

    // Temperature is a sub-object, so we need to get the day temperature
    public double getTemperature() {
      return temperature.getDay();
    }

    public double getWindSpeed() {
      return windSpeed;
    }

    public int getWindDegrees() {
      return windDegrees;
    }

    public double getWindGust() {
      return windGust;
    }

    public int getClouds() {
      return clouds;
    }

    public double getPop() {
      return pop;
    }

    public double getUvi() {
      return uvi;
    }

    public double getRain() {
      return rain != null ? rain : 0.0;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Temp {
    @JsonProperty("day")
    private double day;

    @JsonProperty("min")
    private double min;

    @JsonProperty("max")
    private double max;

    @JsonProperty("night")
    private double night;

    @JsonProperty("eve")
    private double eve;

    @JsonProperty("morn")
    private double morn;

    public Temp() {}

    public double getDay() {
      return day;
    }

    public double getMin() {
      return min;
    }

    public double getMax() {
      return max;
    }

    public double getNight() {
      return night;
    }

    public double getEve() {
      return eve;
    }

    public double getMorn() {
      return morn;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Rain {
    @JsonProperty("1h")
    private double oneHour;

    public Rain() {}

    public double getOneHour() {
      return oneHour;
    }
  }
}
