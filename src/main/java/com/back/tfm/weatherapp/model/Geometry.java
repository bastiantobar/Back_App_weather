package com.back.tfm.weatherapp.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Representa la geometría de un punto")
public class Geometry {
    @Schema(description = "Tipo de geometría, siempre 'Point'")
    private String type = "Point";

    @Schema(description = "Coordenadas del punto [longitud, latitud]")
    private List<Double> coordinates;
}