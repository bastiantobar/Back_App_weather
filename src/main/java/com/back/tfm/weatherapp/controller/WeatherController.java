package com.back.tfm.weatherapp.controller;

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

import java.util.Map;

@RestController
@RequestMapping("/weather")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

   // Endpoint /forecast
    @Operation(summary = "Obtener el pronóstico para Madrid",
            description = "Devuelve datos meteorológicos de pronóstico para la ubicación de Madrid.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Datos obtenidos exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "401", description = "No autorizado, falta autenticación",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Recurso no encontrado",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/forecast")
    @SecurityRequirement(name = "bearerAuth")
    public Mono<ResponseEntity<String>> getForecast() {
        return weatherService.getLocationForecast(40.4168, -3.7038)
                .map(data -> ResponseEntity.ok(data))
                .onErrorResume(e -> {
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Error al obtener el pronóstico: " + e.getMessage()));
                });
    }

    // Endpoint /grafic
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
    @SecurityRequirement(name = "bearerAuth")
    public Mono<ResponseEntity<byte[]>> getMeteogramAsBytes() {
        return weatherService.getMeteogramAsBytes()
                .map(bytes -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, "image/svg+xml")
                        .body(bytes))
                .onErrorResume(e -> {
                    // Generar un JSON para el error
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
