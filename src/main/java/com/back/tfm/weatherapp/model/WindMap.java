package com.back.tfm.weatherapp.model;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Representa una colección de puntos para el mapa interactivo")
public class WindMap {
    private String type = "FeatureCollection";
    private List<WindMapPoint> features;

    // Constructor, Getters y Setters
}
