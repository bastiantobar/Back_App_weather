package com.back.tfm.weatherapp.model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Detalles de los datos instantáneos")

public class InstantWeather {
    private double airTemperature;
    private double relativeHumidity;
    private double airPressureAtSeaLevel;
    private double windSpeed;
    private double cloudAreaFraction;
}
