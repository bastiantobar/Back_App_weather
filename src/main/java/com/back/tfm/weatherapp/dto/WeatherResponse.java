// src/main/java/com/back/tfm/weatherapp/dto/WeatherResponse.java
package com.back.tfm.weatherapp.dto;

import com.back.tfm.weatherapp.model.HourlyForecast;
import com.back.tfm.weatherapp.model.InstantWeather;
import com.back.tfm.weatherapp.model.WindMap; // ¡Importa WindMap!
import io.swagger.v3.oas.annotations.media.Schema; // Agrega esto para Swagger
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta consolidada que contiene todos los datos del clima, calidad del aire y mapa de viento para una ubicación.") // Agrega una descripción para Swagger
public class WeatherResponse {

    @Schema(description = "Coordenadas de la ubicación solicitada.")
    private LocationCoordinates location;

    @Schema(description = "Datos del clima instantáneo para la ubicación.")
    private InstantWeather currentWeather;

    @Schema(description = "Pronóstico horario para las próximas horas.")
    private List<HourlyForecast> hourlyForecasts;

    @Schema(description = "Datos de calidad del aire para la ubicación.")
    private AirQuality airQuality;

    @Schema(description = "Datos para generar un mapa de viento de la ubicación.")
    private WindMap windMap; // <-- ¡ESTO ES LO QUE NECESITAS DESCOMENTAR/AÑADIR!
}