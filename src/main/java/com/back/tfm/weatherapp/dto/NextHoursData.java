// src/main/java/com/back/tfm/weatherapp/dto/NextHoursData.java
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
@Schema(description = "Contiene el resumen y detalles del pronóstico para las próximas horas.")
public class NextHoursData {

    @Schema(description = "Resumen del pronóstico para el período.")
    private NextHoursSummary summary;

    @Schema(description = "Detalles específicos del pronóstico para el período.")
    private NextHoursDetails details;
}