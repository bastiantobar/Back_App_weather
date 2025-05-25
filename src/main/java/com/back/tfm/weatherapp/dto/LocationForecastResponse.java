// src/main/java/com/back/tfm/weatherapp/dto/LocationForecastResponse.java
package com.back.tfm.weatherapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Representa la respuesta completa de la API de pronóstico de ubicación de Met.no")
public class LocationForecastResponse {

    @Schema(description = "Tipo de objeto GeoJSON", example = "FeatureCollection")
    private String type;

    // ¡IMPORTANTE! Cambiamos JsonNode por el DTO fuertemente tipado
    @Schema(description = "Objeto GeoJSON de geometría")
    private LocationForecastGeometry geometry;

    // ¡IMPORTANTE! Cambiamos JsonNode por el DTO fuertemente tipado
    @Schema(description = "Objeto de propiedades, contiene metadatos y series de tiempo de datos")
    private LocationForecastProperties properties;
}