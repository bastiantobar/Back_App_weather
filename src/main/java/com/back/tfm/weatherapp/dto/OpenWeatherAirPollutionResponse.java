// src/main/java/com/back/tfm/weatherapp/dto/OpenWeatherAirPollutionResponse.java
package com.back.tfm.weatherapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map; // Para coord

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenWeatherAirPollutionResponse {
    private Map<String, Double> coord; // Para lat y lon
    private List<AirQualityData> list; // La lista de lecturas de calidad del aire

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AirQualityData {
        private MainData main;
        private Map<String, Double> components;
        private Long dt;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class MainData {
            private Integer aqi;
        }
    }
}