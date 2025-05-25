// src/main/java/com/back/tfm/weatherapp/dto/LocationForecastData.java
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
@Schema(description = "Contiene datos de pronóstico instantáneos y para las próximas horas.")
public class LocationForecastData {

    @Schema(description = "Datos instantáneos del pronóstico.")
    private InstantData instant;

    @Schema(description = "Datos para las próximas 1 hora.")
    private NextHoursData next1Hours;

    @Schema(description = "Datos para las próximas 6 horas.")
    private NextHoursData next6Hours;

    @Schema(description = "Datos para las próximas 12 horas.")
    private NextHoursData next12Hours;
}