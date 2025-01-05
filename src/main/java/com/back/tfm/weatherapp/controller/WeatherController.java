package com.back.tfm.weatherapp.controller;

import com.back.tfm.weatherapp.model.HourlyForecast;
import com.back.tfm.weatherapp.model.InstantWeather;
import com.back.tfm.weatherapp.model.WindMap;
import com.back.tfm.weatherapp.service.WeatherService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/weather")
@SecurityRequirement(name = "bearerAuth") // Requerir token para todos los métodos
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @Operation(summary = "Obtener el gráfico meteorológico (meteograma)",
            description = "Devuelve un gráfico meteorológico en formato SVG para Madrid.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Gráfico obtenido exitosamente",
                    content = @Content(mediaType = "image/svg+xml",
                            schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "401", description = "No autorizado, falta autenticación",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Recurso no encontrado",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/grafic")
    public Mono<ResponseEntity<byte[]>> getMeteogramAsBytes() {
        return weatherService.getMeteogramAsBytes()
                .map(bytes -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, "image/svg+xml")
                        .body(bytes))
                .onErrorResume(e -> {
                    try {
                        Map<String, String> errorResponse = Map.of(
                                "error", "Error al obtener el meteograma",
                                "message", e.getMessage()
                        );
                        byte[] errorJson = new ObjectMapper().writeValueAsBytes(errorResponse);

                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                                .body(errorJson));
                    } catch (Exception ex) {
                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                                .body(("{\"error\":\"Error interno\"}").getBytes()));
                    }
                });
    }

    @Operation(summary = "Obtener información meteorológica instantánea",
            description = "Devuelve la información meteorológica actual para Madrid.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Datos obtenidos exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = InstantWeather.class))),
            @ApiResponse(responseCode = "401", description = "No autorizado, falta autenticación",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/instant")
    public Mono<ResponseEntity<InstantWeather>> getInstantWeather() {
        return weatherService.getInstantWeather()
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .build()));
    }

    @Operation(summary = "Obtener pronóstico por horas",
            description = "Devuelve el pronóstico meteorológico por horas para las próximas 48 horas en Madrid.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Datos obtenidos exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = HourlyForecast.class))),
            @ApiResponse(responseCode = "401", description = "No autorizado, falta autenticación",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/hourly")
    public Mono<ResponseEntity<List<HourlyForecast>>> getHourlyForecast() {
        return weatherService.getHourlyForecast()
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .build()));
    }

    @Operation(summary = "Obtener mapa de velocidad del viento",
            description = "Devuelve un mapa que muestra la velocidad del viento en Madrid.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Datos obtenidos exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = WindMap.class))),
            @ApiResponse(responseCode = "401", description = "No autorizado, falta autenticación",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/wind-map")
    public Mono<ResponseEntity<WindMap>> getWindSpeedMap() {
        return weatherService.getWindSpeedMap()
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .build()));
    }
}
