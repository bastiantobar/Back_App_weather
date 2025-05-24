// src/main/java/com/back/tfm/weatherapp/dto/LocationForecastResponse.java
package com.back.tfm.weatherapp.dto;

import com.fasterxml.jackson.databind.JsonNode;
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

    @Schema(description = "Objeto GeoJSON de geometría", example = "{\"type\": \"Point\", \"coordinates\": [-3.7038, 40.4168, 100.0]}")
    private JsonNode geometry; // Capturará la sección "geometry" como un JsonNode

    @Schema(description = "Objeto de propiedades, contiene metadatos y series de tiempo de datos", example = "{\"meta\": {...}, \"timeseries\": [...]}")
    private JsonNode properties; // Capturará la sección "properties" como un JsonNode, que contiene "timeseries"
}