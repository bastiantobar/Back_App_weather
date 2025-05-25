package com.back.tfm.weatherapp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties; // <-- ¡Añade esta importación!
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // <-- ¡Añade esta anotación!
@Schema(description = "Contiene metadatos y una serie de tiempo de datos de pronóstico.")
public class LocationForecastProperties {

    @Schema(description = "Metadatos del pronóstico.")
    private LocationForecastMeta meta;

    @Schema(description = "Serie de tiempo de datos de pronóstico.")
    private List<TimeseriesData> timeseries;
}