/*
 * Copyright 2025, OpenRemote Inc.
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
package org.openremote.agent.protocol.openweathermap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * The response object from the OpenWeatherMap API, trimmed down to only include
 * the core weather properties
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenWeatherMapResponse {

    @JsonProperty("current")
    private WeatherDatapoint current;

    @JsonProperty("hourly")
    private List<WeatherDatapoint> hourly;

    @JsonProperty("daily")
    private List<DailyWeatherDatapoint> daily;

    public OpenWeatherMapResponse() {
    }

    public WeatherDatapoint getCurrent() {
        return current;
    }

    public List<WeatherDatapoint> getHourly() {
        return hourly;
    }

    public List<DailyWeatherDatapoint> getDaily() {
        return daily;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WeatherDatapoint {

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

        public WeatherDatapoint() {
        }

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
    public static class DailyWeatherDatapoint {
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

        public DailyWeatherDatapoint() {
        }

        public long getTimestamp() {
            return timestamp;
        }

        public int getPressure() {
            return pressure;
        }

        public int getHumidity() {
            return humidity;
        }

        public Temp getTemperature() {
            return temperature;
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

        public Temp() {
        }

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

        public Rain() {
        }

        public double getOneHour() {
            return oneHour;
        }
    }

}
