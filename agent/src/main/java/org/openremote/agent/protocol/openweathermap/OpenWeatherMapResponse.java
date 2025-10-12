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
    private WeatherEntry current;

    @JsonProperty("hourly")
    private List<WeatherEntry> hourly;

    @JsonProperty("daily")
    private List<DailyWeatherEntry> daily;

    /**
     * For hydrators
     */
    public OpenWeatherMapResponse() {
    }

    public WeatherEntry getCurrent() {
        return current;
    }

    public void setCurrent(WeatherEntry current) {
        this.current = current;
    }


    public List<WeatherEntry> getHourly() {
        return hourly;
    }

    public void setHourly(List<WeatherEntry> hourly) {
        this.hourly = hourly;
    }

    public List<DailyWeatherEntry> getDaily() {
        return daily;
    }

    public void setDaily(List<DailyWeatherEntry> daily) {
        this.daily = daily;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WeatherEntry {

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

        /**
         * For hydrators
         */
        public WeatherEntry() {
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public int getPressure() {
            return pressure;
        }

        public void setPressure(int pressure) {
            this.pressure = pressure;
        }

        public int getHumidity() {
            return humidity;
        }

        public void setHumidity(int humidity) {
            this.humidity = humidity;
        }

        public int getClouds() {
            return clouds;
        }

        public void setClouds(int clouds) {
            this.clouds = clouds;
        }

        public double getWindSpeed() {
            return windSpeed;
        }

        public void setWindSpeed(double windSpeed) {
            this.windSpeed = windSpeed;
        }

        public int getWindDegrees() {
            return windDegrees;
        }

        public void setWindDegrees(int windDegrees) {
            this.windDegrees = windDegrees;
        }

        public double getWindGust() {
            return windGust;
        }

        public void setWindGust(double windGust) {
            this.windGust = windGust;
        }

        public double getPop() {
            return pop;
        }

        public void setPop(double pop) {
            this.pop = pop;
        }

        public double getUvi() {
            return uvi;
        }

        public void setUvi(double uvi) {
            this.uvi = uvi;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DailyWeatherEntry {
        @JsonProperty("dt")
        private long timestamp;

        @JsonProperty("temp")
        private Temp temp;

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
        private double rain;

        @JsonProperty("uvi")
        private double uvi;

        public DailyWeatherEntry() {
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public Temp getTemp() {
            return temp;
        }

        public void setTemp(Temp temp) {
            this.temp = temp;
        }

        public int getPressure() {
            return pressure;
        }

        public void setPressure(int pressure) {
            this.pressure = pressure;
        }

        public int getHumidity() {
            return humidity;
        }

        public void setHumidity(int humidity) {
            this.humidity = humidity;
        }

        public double getWindSpeed() {
            return windSpeed;
        }

        public void setWindSpeed(double windSpeed) {
            this.windSpeed = windSpeed;
        }

        public int getWindDegrees() {
            return windDegrees;
        }

        public void setWindDegrees(int windDegrees) {
            this.windDegrees = windDegrees;
        }

        public double getWindGust() {
            return windGust;
        }

        public void setWindGust(double windGust) {
            this.windGust = windGust;
        }

        public int getClouds() {
            return clouds;
        }

        public void setClouds(int clouds) {
            this.clouds = clouds;
        }

        public double getPop() {
            return pop;
        }

        public void setPop(double pop) {
            this.pop = pop;
        }

        public double getRain() {
            return rain;
        }

        public void setRain(double rain) {
            this.rain = rain;
        }

        public double getUvi() {
            return uvi;
        }

        public void setUvi(double uvi) {
            this.uvi = uvi;
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

        public void setDay(double day) {
            this.day = day;
        }

        public double getMin() {
            return min;
        }

        public void setMin(double min) {
            this.min = min;
        }

        public double getMax() {
            return max;
        }

        public void setMax(double max) {
            this.max = max;
        }

        public double getNight() {
            return night;
        }

        public void setNight(double night) {
            this.night = night;
        }

        public double getEve() {
            return eve;
        }

        public void setEve(double eve) {
            this.eve = eve;
        }

        public double getMorn() {
            return morn;
        }

        public void setMorn(double morn) {
            this.morn = morn;
        }
    }

}
