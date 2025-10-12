package org.openremote.agent.protocol.openweathermap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;


/**
 * The response object from the OpenWeatherMap API,
 * trimmed down to only include the core weather properties
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenWeatherMapResponse {
    
    @JsonProperty("list")
    private List<WeatherEntry> list;


    /**
     * For hydrators
     */
    public OpenWeatherMapResponse() {
    }

    public List<WeatherEntry> getList() {
        return list;
    }

    public void setList(List<WeatherEntry> list) {
        this.list = list;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WeatherEntry {
        
        @JsonProperty("dt")
        private long timestamp;
        
        @JsonProperty("main")
        private Main main;
        
        @JsonProperty("clouds")
        private Clouds clouds;
        
        @JsonProperty("wind")
        private Wind wind;

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

        public Main getMain() {
            return main;
        }

        public void setMain(Main main) {
            this.main = main;
        }

        public Clouds getClouds() {
            return clouds;
        }

        public void setClouds(Clouds clouds) {
            this.clouds = clouds;
        }

        public Wind getWind() {
            return wind;
        }

        public void setWind(Wind wind) {
            this.wind = wind;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Main {
        
        @JsonProperty("temp")
        private double temperature;
        
        @JsonProperty("feels_like")
        private double feelsLike;
        
        @JsonProperty("temp_min")
        private double temperatureMin;
        
        @JsonProperty("temp_max")
        private double temperatureMax;
        
        @JsonProperty("pressure")
        private int pressure;
        
        @JsonProperty("humidity")
        private int humidity;

        /**
         * For hydrators
         */
        public Main() {
        }
        
        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public double getFeelsLike() {
            return feelsLike;
        }

        public void setFeelsLike(double feelsLike) {
            this.feelsLike = feelsLike;
        }

        public double getTemperatureMin() {
            return temperatureMin;
        }

        public void setTemperatureMin(double temperatureMin) {
            this.temperatureMin = temperatureMin;
        }

        public double getTemperatureMax() {
            return temperatureMax;
        }

        public void setTemperatureMax(double temperatureMax) {
            this.temperatureMax = temperatureMax;
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
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Clouds {
        
        @JsonProperty("all")
        private int all;

        /**
         * For hydrators
         */
        public Clouds() {
        }

        public int getAll() {
            return all;
        }

        public void setAll(int all) {
            this.all = all;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Wind {
        
        @JsonProperty("speed")
        private double speed;
        
        @JsonProperty("deg")
        private int degrees;
        
        @JsonProperty("gust")
        private double gust;

        /**
         * For hydrators
         */
        public Wind() {
        }

        public double getSpeed() {
            return speed;
        }

        public void setSpeed(double speed) {
            this.speed = speed;
        }

        public int getDegrees() {
            return degrees;
        }

        public void setDegrees(int degrees) {
            this.degrees = degrees;
        }

        public double getGust() {
            return gust;
        }

        public void setGust(double gust) {
            this.gust = gust;
        }
    }

}
