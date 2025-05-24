package com.back.tfm.weatherapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties; // <--- ¡Importa esta anotación!
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
    private Results results;
    @Schema(description = "Estado de la solicitud (e.g., 'OK')")
    private String status;
    @Schema(description = "Zona horaria en la que se calculan los resultados (UTC)")
    private String tzid; // Agregado según el ejemplo

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true) // <--- ¡Añade esta línea aquí!
    @Schema(description = "Detalles de los tiempos astronómicos")
    public static class Results {
        @Schema(description = "Hora de salida del sol en formato HH:MM:SS AM/PM")
        private String sunrise;
        @Schema(description = "Hora de puesta del sol en formato HH:MM:SS AM/PM")
        private String sunset;
        @JsonProperty("solar_noon")
        @Schema(description = "Hora del mediodía solar (cuando el sol está en su punto más alto)")
        private String solarNoon;
        @JsonProperty("day_length")
        @Schema(description = "Duración del día en formato HH:MM:SS")
        private String dayLength; // Este campo ya está mapeado a "day_length" de JSON
        @JsonProperty("civil_twilight_begin")
        @Schema(description = "Inicio del crepúsculo civil")
        private String civilTwilightBegin;
        @JsonProperty("civil_twilight_end")
        @Schema(description = "Fin del crepúsculo civil")
        private String civilTwilightEnd;
        @JsonProperty("nautical_twilight_begin")
        @Schema(description = "Inicio del crepúsculo náutico")
        private String nauticalTwilightBegin;
        @JsonProperty("nautical_twilight_end")
        @Schema(description = "Fin del crepúsculo náutico")
        private String nauticalTwilightEnd;
        @JsonProperty("astronomical_twilight_begin")
        @Schema(description = "Inicio del crepúsculo astronómico")
        private String astronomicalTwilightBegin;
        @JsonProperty("astronomical_twilight_end")
        @Schema(description = "Fin del crepúsculo astronómico")
        private String astronomicalTwilightEnd;
    }
}