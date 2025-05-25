// src/main/java/com/back/tfm/weatherapp/dto/NextHoursDetails.java
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
@Schema(description = "Detalles del pronóstico para las próximas horas.")
public class NextHoursDetails {

    @Schema(description = "Cantidad total de precipitación para el período en milímetros.", example = "0.5")
    private Double precipitation_amount;
    // Puedes añadir más campos según necesites del JSON de Met.no para 'next_1_hours', 'next_6_hours', 'next_12_hours'
}