package com.back.tfm.weatherapp.model;

import java.util.List;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Representa los datos necesarios para el mapa de velocidad del viento")

public class WindMapPoint {
    private List<Double> coordinates; // [longitude, latitude]
    private double windSpeed;

    // Getters y Setters
}
