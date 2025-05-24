package com.back.tfm.weatherapp.dto;

import com.back.tfm.weatherapp.model.HourlyForecast;
import com.back.tfm.weatherapp.model.InstantWeather;
import com.back.tfm.weatherapp.model.WindMap;
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
@Schema(description = "Respuesta consolidada que contiene todos los datos del clima, calidad del aire y mapa de viento para una ubicación.")
public class WeatherResponse {

    @Schema(description = "Coordenadas de la ubicación solicitada.")
    private LocationCoordinates location;

    @Schema(description = "Datos del clima instantáneo para la ubicación.")
    private InstantWeather currentWeather; // Nombre corregido previamente

    @Schema(description = "Pronóstico horario para las próximas horas.")
    private List<HourlyForecast> hourlyForecasts;

    @Schema(description = "Datos de calidad del aire para la ubicación.")
    private AirQuality airQuality;

    @Schema(description = "Datos para generar un mapa de viento de la ubicación.")
    private WindMap windMap;

    @Schema(description = "Datos de salida y puesta del sol, y crepúsculos.")
    private SunriseSunsetResponse.Results astronomicalTimes; // <-- ¡NUEVO CAMPO!
}