// src/main/java/com/back/tfm/weatherapp/dto/ForecastUnits.java
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
@Schema(description = "Unidades de las variables de pronóstico de Met.no")
public class ForecastUnits {
    @Schema(description = "Unidad para la temperatura del aire", example = "celsius")
    private String air_temperature;
    @Schema(description = "Unidad para la velocidad del viento", example = "m/s")
    private String wind_speed;
    @Schema(description = "Unidad para la dirección del viento", example = "degrees")
    private String wind_from_direction;
    @Schema(description = "Unidad para la presión del aire a nivel del mar", example = "hPa")
    private String air_pressure_at_sea_level;
    @Schema(description = "Unidad para la fracción de área de nubes", example = "%")
    private String cloud_area_fraction;
    @Schema(description = "Unidad para la humedad relativa", example = "%")
    private String relative_humidity;
    @Schema(description = "Unidad para la cantidad de precipitación", example = "mm")
    private String precipitation_amount;
}