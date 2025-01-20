package com.back.tfm.weatherapp.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Representa una colección de puntos para el mapa interactivo")
public class WindMap {
    @Schema(description = "Tipo de colección GeoJSON, siempre 'FeatureCollection'")
    private String type = "FeatureCollection";

    @Schema(description = "Lista de características individuales en el mapa")
    private List<WindMapPoint> features;
}
