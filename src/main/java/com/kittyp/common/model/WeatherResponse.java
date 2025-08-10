package com.kittyp.common.model;

import lombok.Data;
import java.time.ZonedDateTime;

@Data
public class WeatherResponse {

    private ZonedDateTime currentTime;
    private TimeZoneInfo timeZone;
    private boolean isDaytime;
    private WeatherCondition weatherCondition;
    private Temperature temperature;
    private Temperature feelsLikeTemperature;
    private Temperature dewPoint;
    private Temperature heatIndex;
    private Temperature windChill;
    private int relativeHumidity;
    private int uvIndex;
    private Precipitation precipitation;
    private int thunderstormProbability;
    private AirPressure airPressure;
    private Wind wind;
    private Visibility visibility;
    private int cloudCover;
    private CurrentConditionsHistory currentConditionsHistory;

    @Data
    public static class TimeZoneInfo {
        private String id;
    }

    @Data
    public static class WeatherCondition {
        private String iconBaseUri;
        private Description description;
        private String type;

        @Data
        public static class Description {
            private String text;
            private String languageCode;
        }
    }

    @Data
    public static class Temperature {
        private double degrees;
        private String unit;
    }

    @Data
    public static class Precipitation {
        private Probability probability;
        private Quantity snowQpf;
        private Quantity qpf;

        @Data
        public static class Probability {
            private int percent;
            private String type;
        }

        @Data
        public static class Quantity {
            private double quantity;
            private String unit;
        }
    }

    @Data
    public static class AirPressure {
        private double meanSeaLevelMillibars;
    }

    @Data
    public static class Wind {
        private Direction direction;
        private Speed speed;
        private Speed gust;

        @Data
        public static class Direction {
            private int degrees;
            private String cardinal;
        }

        @Data
        public static class Speed {
            private double value;
            private String unit;
        }
    }

    @Data
    public static class Visibility {
        private double distance;
        private String unit;
    }

    @Data
    public static class CurrentConditionsHistory {
        private Temperature temperatureChange;
        private Temperature maxTemperature;
        private Temperature minTemperature;
        private Precipitation.Quantity snowQpf;
        private Precipitation.Quantity qpf;
    }
}

