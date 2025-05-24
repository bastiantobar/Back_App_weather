package com.back.tfm.weatherapp.controller;

import com.back.tfm.weatherapp.dto.LocationCoordinates;
import com.back.tfm.weatherapp.model.HourlyForecast;
import com.back.tfm.weatherapp.model.InstantWeather;
import com.back.tfm.weatherapp.model.WindMap;
import com.back.tfm.weatherapp.service.FirebaseRealtimeService;
import com.back.tfm.weatherapp.service.GeocodingService;
import com.back.tfm.weatherapp.service.WeatherService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/weather")
public class WeatherController {

    private final FirebaseRealtimeService firebaseRealtimeService;
    private final WeatherService weatherService;
    private final GeocodingService geocodingService;

    public WeatherController(FirebaseRealtimeService firebaseRealtimeService, WeatherService weatherService,GeocodingService geocodingService) {
        this.firebaseRealtimeService = firebaseRealtimeService;
        this.weatherService = weatherService;
        this.geocodingService = geocodingService;
    }

    @Operation(
            summary = "Obtener el último dato de clima instantáneo",
            description = "Devuelve el último registro de clima instantáneo almacenado en Firebase."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dato obtenido exitosamente",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"temperature\": 25.5, \"humidity\": 80, \"windSpeed\": 15 }"),
                            schema = @Schema(implementation = InstantWeather.class))),
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

    @GetMapping("/hourly")
    @Operation(
            summary = "Obtener pronósticos por hora (últimas 48 horas)",
            description = "Devuelve los registros de pronósticos horarios almacenados en Firebase dentro de las últimas 48 horas."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Datos obtenidos exitosamente",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "[ { \"time\": \"2025-01-31T10:00:00Z\", \"temperature\": 18.3, \"humidity\": 75, \"windSpeed\": 10 }, { \"time\": \"2025-01-31T11:00:00Z\", \"temperature\": 19.1, \"humidity\": 73, \"windSpeed\": 12 } ]"),
                            schema = @Schema(implementation = HourlyForecast.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json"))
    })
    public Mono<ResponseEntity<List<HourlyForecast>>> getHourlyForecastsLast48Hours() {
        return firebaseRealtimeService.getHourlyForecastsLast48Hours()
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .build()));
    }

    @Operation(
            summary = "Obtener el último punto del mapa de viento",
            description = "Devuelve solo el registro más reciente del mapa de viento almacenado en Firebase."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registro más reciente obtenido exitosamente",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"features\": [ { \"geometry\": { \"coordinates\": [ -3.7038, 40.4168 ] }, \"properties\": { \"windSpeed\": 12, \"windDirection\": 270 } } ] }"),
                            schema = @Schema(implementation = WindMap.class))),
            @ApiResponse(responseCode = "204", description = "No hay registros disponibles"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/wind-map/last")
    public Mono<ResponseEntity<?>> getLastWindMap() {
        return firebaseRealtimeService.getLastWindMap()
                .map(windMap -> {
                    if (windMap == null || windMap.getFeatures() == null || windMap.getFeatures().isEmpty()) {
                        return ResponseEntity.noContent().build(); // Devuelve 204 si no hay datos
                    }
                    return ResponseEntity.ok(windMap); // Devuelve el registro más reciente
                })
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).<WindMap>build())); // Especificación explícita del tipo
    }

    @Operation(
            summary = "Obtener el gráfico meteorológico (meteograma)",
            description = "Devuelve un gráfico meteorológico en formato SVG para Madrid."
    )
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

    /**
     * Este es el endpoint para obtener solo las coordenadas de una ubicación.
     * Es independiente de los datos de clima, aire, etc.
     */
    @Operation(
            summary = "Obtener coordenadas geográficas por ciudad y país",
            description = "Dado el nombre de una ciudad y país, devuelve sus coordenadas geográficas (latitud, longitud), nombre completo y código de país. (Firebase bypass activo para depuración)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Coordenadas obtenidas exitosamente",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\n  \"name\": \"Hijuelas, Región de Valparaíso, Chile\",\n  \"latitude\": -32.8339845,\n  \"longitude\": -71.1895697,\n  \"countryCode\": \"CL\"\n}"),
                            schema = @Schema(implementation = LocationCoordinates.class))),
            @ApiResponse(responseCode = "400", description = "Parámetros de entrada inválidos (ej. ciudad/país faltante o no encontrado)",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{}"))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor al procesar la solicitud de geocodificación",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{}")))
    })
    @GetMapping("/location") // El endpoint final para las coordenadas
    public Mono<ResponseEntity<LocationCoordinates>> getLocationCoordinates(
            @Parameter(description = "Nombre de la ciudad.", required = true, example = "Hijuelas")
            @RequestParam String city,
            @Parameter(description = "Nombre del país.", required = true, example = "Chile")
            @RequestParam String country) {

        System.out.println(">>> [Controller] Recibida solicitud para /location");
        System.out.println(">>> [Controller] Ciudad: '" + city + "', País: '" + country + "'");

        if (city == null || city.trim().isEmpty() || country == null || country.trim().isEmpty()) {
            System.err.println("<<< [Controller] Error 400: Parámetros de ciudad o país vacíos.");
            return Mono.just(ResponseEntity.badRequest().build()); // Retorna 400 Bad Request
        }

        // Llama al GeocodingService para obtener las coordenadas
        System.out.println(">>> [Controller] Llamando a GeocodingService.getCoordinates...");
        return geocodingService.getCoordinates(city, country)
                .map(coords -> {
                    System.out.println("<<< [Controller] GeocodingService completado exitosamente. Coordenadas obtenidas: " + coords);
                    return ResponseEntity.ok(coords); // Si éxito, retorna 200 OK con el DTO
                })
                .onErrorResume(IllegalArgumentException.class, e -> {
                    System.err.println("<<< [Controller] Error 400: Ubicación no encontrada por GeocodingService. Mensaje: " + e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).build()); // Retorna 400
                })
                .onErrorResume(e -> {
                    System.err.println("<<< [Controller] Error 500: Fallo inesperado en /location. Mensaje: " + e.getMessage());
                    e.printStackTrace();
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()); // Retorna 500
                })
                .doFinally(signalType -> {
                    System.out.println("--- [Controller] Solicitud /location finalizada con estado: " + signalType + " ---");
                });
    }


}
