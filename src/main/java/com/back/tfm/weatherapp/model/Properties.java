package com.back.tfm.weatherapp.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Propiedades del punto, incluye velocidad y dirección del viento")
public class Properties {
    @Schema(description = "Velocidad del viento en m/s")
    private double windSpeed;

    @Schema(description = "Dirección del viento en grados")
    private double windDirection;

    @Schema(description = "Hora de la medición en formato ISO 8601")
    private String time;
}
