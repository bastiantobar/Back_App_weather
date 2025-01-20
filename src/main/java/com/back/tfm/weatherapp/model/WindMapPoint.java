package com.back.tfm.weatherapp.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Representa un punto de datos en el mapa de velocidad del viento")
public class WindMapPoint {
    @Schema(description = "Geometría del punto, incluye coordenadas")
    private Geometry geometry;

    @Schema(description = "Propiedades asociadas al punto, como velocidad y dirección del viento")
    private Properties properties;

    @Schema(description = "Tipo del elemento GeoJSON, siempre 'Feature'")
    private String type = "Feature";
}
