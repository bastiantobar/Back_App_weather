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
@Schema(description = "Representa la geometría GeoJSON de la ubicación del pronóstico de Met.no")
public class LocationForecastGeometry {

    @Schema(description = "Tipo de geometría GeoJSON, debe ser 'Point'", example = "Point")
    private String type;

    @Schema(description = "Coordenadas del punto [longitud, latitud, altitud (opcional)]", example = "[-3.7038, 40.4168, 100.0]")
    private List<Double> coordinates;
}