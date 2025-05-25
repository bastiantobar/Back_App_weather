// src/main/java/com/back/tfm/weatherapp/dto/NextHoursSummary.java
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
@Schema(description = "Resumen del pronóstico para las próximas horas, incluye el símbolo meteorológico.")
public class NextHoursSummary {

    @Schema(description = "Código del símbolo meteorológico que representa las condiciones predominantes.", example = "clearsky_day")
    private String symbol_code;
}