// src/main/java/com/back/tfm/weatherapp/dto/TimeseriesData.java
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
@Schema(description = "Representa una entrada de datos de la serie de tiempo del pronóstico de Met.no")
public class TimeseriesData {

    @Schema(description = "Marca de tiempo de la entrada del pronóstico en formato ISO 8601.")
    private String time;

    @Schema(description = "Contiene datos de pronóstico instantáneos y para las próximas horas.")
    private LocationForecastData data; // ¡Este es el tipo correcto!
}