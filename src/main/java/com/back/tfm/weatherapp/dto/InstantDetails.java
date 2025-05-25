// src/main/java/com/back/tfm/weatherapp/dto/InstantDetails.java
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
@Schema(description = "Detalles de los datos instantáneos")
public class InstantDetails {
    @Schema(description = "Temperatura del aire en grados Celsius.", example = "15.2")
    private Double air_temperature;
    @Schema(description = "Humedad relativa en porcentaje.", example = "85.0")
    private Double relative_humidity;
    @Schema(description = "Presión del aire a nivel del mar en hPa.", example = "1012.5")
    private Double air_pressure_at_sea_level;
    @Schema(description = "Velocidad del viento en metros por segundo.", example = "3.1")
    private Double wind_speed;
    @Schema(description = "Dirección del viento en grados (0-360).", example = "270.0")
    private Double wind_from_direction;
    @Schema(description = "Fracción de área de nubes en porcentaje.", example = "50.0")
    private Double cloud_area_fraction;
    // Puedes añadir más campos según necesites del JSON de Met.no
}