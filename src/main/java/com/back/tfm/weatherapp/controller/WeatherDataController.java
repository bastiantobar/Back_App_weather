package com.back.tfm.weatherapp.controller;

import com.back.tfm.weatherapp.model.HourlyForecast;
import com.back.tfm.weatherapp.model.InstantWeather;
import com.back.tfm.weatherapp.model.WindMap;
import com.back.tfm.weatherapp.service.FirebaseRealtimeService;
import com.back.tfm.weatherapp.service.WeatherService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
public class WeatherDataController {

    private final FirebaseRealtimeService firebaseRealtimeService;
    private final WeatherService weatherService;

    public WeatherDataController(FirebaseRealtimeService firebaseRealtimeService,WeatherService weatherService) {
        this.firebaseRealtimeService = firebaseRealtimeService;
        this.weatherService = weatherService;
    }

    @Operation(
            summary = "Obtener datos de clima instantáneo",
            description = "Devuelve todos los registros de clima instantáneo almacenados en Firebase."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Datos obtenidos exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = InstantWeather.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/instant")
    public Mono<ResponseEntity<List<InstantWeather>>> getAllInstantWeather() {
        return firebaseRealtimeService.getAllInstantWeather()
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .build()));
    }
    @Operation(
            summary = "Obtener el último dato de clima instantáneo",
            description = "Devuelve el último registro de clima instantáneo almacenado en Firebase."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dato obtenido exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = InstantWeather.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/instant/last")
    public Mono<ResponseEntity<InstantWeather>> getLastInstantWeather() {
        return firebaseRealtimeService.getLastInstantWeather()
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .build()));
    }


    @Operation(
            summary = "Obtener pronósticos por hora",
            description = "Devuelve todos los registros de pronósticos horarios almacenados en Firebase."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Datos obtenidos exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = HourlyForecast.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/hourly")
    public Mono<ResponseEntity<List<HourlyForecast>>> getAllHourlyForecasts() {
        return firebaseRealtimeService.getAllHourlyForecasts()
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .build()));
    }

    @Operation(
            summary = "Obtener mapas de viento",
            description = "Devuelve todos los registros de mapas de viento almacenados en Firebase."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Datos obtenidos exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = WindMap.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/wind-maps")
    public Mono<ResponseEntity<List<WindMap>>> getAllWindMaps() {
        return firebaseRealtimeService.getAllWindMaps()
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .build()));
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
}
