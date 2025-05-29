package com.back.tfm.weatherapp.model;

import com.back.tfm.weatherapp.dto.AirQuality;
import com.back.tfm.weatherapp.dto.LocationCoordinates;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Registro histórico de datos meteorológicos para un lugar y tiempo específicos")
public class HistoricalWeatherEntry {

    @Schema(description = "ID único del registro histórico (generado por Firebase)", example = "-NsAcX_Y6Z-yE9fC0aBc")
    private String id;

    @Schema(description = "Datos de la ubicación geográfica", required = true)
    private LocationCoordinates location;

    // --- ¡¡¡MODIFICACIÓN DEL PATRÓN AQUÍ!!! ---
    // Usamos 'SSSSSS' para seis dígitos de fracciones de segundo, o 'SSSSSSSSS' para hasta nueve (nanosegundos).
    // O puedes eliminar el 'pattern' y confiar en el ToStringSerializer si JavaTimeModule lo maneja bien.
    // Probemos con SSSSSS primero. Si hay más precision, podríamos necesitar SSSSSSSSS
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", timezone = "UTC")
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "Timestamp en UTC cuando se registró esta entrada histórica", example = "2025-05-28T15:30:00.123456Z", required = true)
    private Instant recordedAt;
    // --- FIN DEL CAMBIO ---

    @Schema(description = "Resumen de las condiciones instantáneas de este registro")
    private InstantWeather instantWeatherSnapshot;

    @Schema(description = "Lista de pronósticos horarios para el día de este registro")
    private List<HourlyForecast> hourlyForecasts;

    @Schema(description = "Datos de calidad del aire asociados a este registro")
    private AirQuality airQualitySnapshot;
}