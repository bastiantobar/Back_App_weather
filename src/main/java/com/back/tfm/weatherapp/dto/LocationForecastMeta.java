// src/main/java/com/back/tfm/weatherapp/dto/LocationForecastMeta.java
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
@Schema(description = "Metadatos de la respuesta del pronóstico de Met.no")
public class LocationForecastMeta {

    @Schema(description = "Marca de tiempo de la última actualización en formato ISO 8601.", example = "2023-10-26T12:00:00Z")
    private String updated_at;

    @Schema(description = "Unidades de las variables de pronóstico.")
    private ForecastUnits units;
}