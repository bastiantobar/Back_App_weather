// src/main/java/com/back/tfm/weatherapp/dto/AirQuality.java
package com.back.tfm.weatherapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty; // Importar para mapeo JSON

import java.util.Map; // Para los componentes de contaminantes

@Data
@Builder
@NoArgsConstructor // Necesario para Firebase y para la deserialización JSON
@AllArgsConstructor // Útil para el Builder de Lombok
public class AirQuality {

    // No necesitamos lat/lon aquí si ya los tenemos en LocationCoordinates
    // Pero si quieres que cada DTO tenga sus propias coordenadas, déjalos.
    // Para simplificar, podemos asumir que se pasan desde LocationCoordinates
    // o se extraen del "coord" de la respuesta RAW y se asignan.
    private Double latitude;
    private Double longitude;

    // El índice principal de calidad del aire (de 1 a 5 según OWM)
    // Mapea directamente de "main.aqi" de la respuesta
    @JsonProperty("aqi") // Mapea el campo 'aqi' dentro del objeto 'main'
    private Integer aqi;

    // Los componentes de contaminantes y sus concentraciones
    // Mapea directamente del objeto "components"
    private Map<String, Double> components;

    // El timestamp de la lectura (en segundos Unix)
    // Mapea directamente de "dt" de la respuesta
    private Long timestamp;

    // Podemos añadir una descripción amigable basada en el 'aqi'
    // Esto lo calcularemos en nuestro servicio, no viene directamente de la API como 'aqiDescription'
    private String aqiCategory; // Ej: "Good", "Fair", "Moderate", "Poor", "Very Poor"

    // Constructor vacío requerido por Firebase y por Jackson para deserialización
    // @NoArgsConstructor ya lo genera

    // Constructor completo para el builder
    // @AllArgsConstructor ya lo genera

    // Método de utilidad para obtener la descripción de la categoría (puedes moverlo a un util si prefieres)
    public static String getAqiCategory(Integer aqiValue) {
        if (aqiValue == null) {
            return "N/A";
        }
        switch (aqiValue) {
            case 1: return "Good";
            case 2: return "Fair";
            case 3: return "Moderate";
            case 4: return "Poor";
            case 5: return "Very Poor";
            default: return "Unknown";
        }
    }
}