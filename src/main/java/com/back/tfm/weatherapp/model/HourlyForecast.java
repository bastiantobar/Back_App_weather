package com.back.tfm.weatherapp.model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Detalles del pronóstico por hora")
public class HourlyForecast {
    private String time;
    private double airTemperature;
    private double windSpeed;
    private double precipitationAmount;
}
