package com.back.tfm.weatherapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty; // ¡CRUCIAL!
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta completa de la API de Sunrise-Sunset.org")
public class SunriseSunsetResponse {
    @Schema(description = "Resultados de los cálculos de salida/puesta del sol y crepúsculos")
    @JsonProperty("results") // Asegúrate de que el campo "results" en el JSON se mapee
    private Results results;
    @Schema(description = "Estado de la solicitud (e.g., 'OK')")
    private String status;
    @Schema(description = "Zona horaria en la que se calculan los resultados (UTC)")
    private String tzid;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "Detalles de los tiempos astronómicos")
    public static class Results {
        @JsonProperty("sunrise")
        @Schema(description = "Hora de salida del sol en formato HH:MM:SS AM/PM")
        private String sunrise;
        @JsonProperty("sunset")
        @Schema(description = "Hora de puesta del sol en formato HH:MM:SS AM/PM")
        private String sunset;
        @JsonProperty("solar_noon") // ¡CRUCIAL: Mapea "solar_noon" de Firebase a solarNoon!
        @Schema(description = "Hora del mediodía solar (cuando el sol está en su punto más alto)")
        private String solarNoon;
        @JsonProperty("day_length") // ¡CRUCIAL: Mapea "day_length" de Firebase a dayLength!
        @Schema(description = "Duración del día en formato HH:MM:SS")
        private String dayLength;
        @JsonProperty("civil_twilight_begin") // ¡CRUCIAL!
        @Schema(description = "Inicio del crepúsculo civil")
        private String civilTwilightBegin;
        @JsonProperty("civil_twilight_end") // ¡CRUCIAL!
        @Schema(description = "Fin del crepúsculo civil")
        private String civilTwilightEnd;
        @JsonProperty("nautical_twilight_begin") // ¡CRUCIAL!
        @Schema(description = "Inicio del crepúsculo náutico")
        private String nauticalTwilightBegin;
        @JsonProperty("nautical_twilight_end") // ¡CRUCIAL!
        @Schema(description = "Fin del crepúsculo náutico")
        private String nauticalTwilightEnd;
        @JsonProperty("astronomical_twilight_begin") // ¡CRUCIAL!
        @Schema(description = "Inicio del crepúsculo astronómico")
        private String astronomicalTwilightBegin;
        @JsonProperty("astronomical_twilight_end") // ¡CRUCIAL!
        @Schema(description = "Fin del crepúsculo astronómico")
        private String astronomicalTwilightEnd;
    }
}