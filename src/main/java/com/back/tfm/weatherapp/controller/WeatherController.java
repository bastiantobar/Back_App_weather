// src/main/java/com/back/tfm/weatherapp/controller/WeatherController.java
package com.back.tfm.weatherapp.controller;

import com.back.tfm.weatherapp.dto.LocationCoordinates;
import com.back.tfm.weatherapp.dto.WeatherResponse; // Importa tu DTO de respuesta consolidada
import com.back.tfm.weatherapp.model.ErrorResponse; // Importa tu modelo de respuesta de error
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

    public WeatherController(FirebaseRealtimeService firebaseRealtimeService, WeatherService weatherService, GeocodingService geocodingService) {
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

    @Operation(summary = "Obtiene un reporte meteorológico completo (actuales, pronóstico por hora, mapa de viento y calidad del aire) para una ubicación específica.",
            description = "Combina llamadas a servicios de geocodificación y pronóstico meteorológico para proporcionar un conjunto completo de datos. Incluye manejo de caché para mejorar el rendimiento.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Datos meteorológicos obtenidos exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = com.back.tfm.weatherapp.dto.WeatherResponse.class),
                            examples = @ExampleObject(value = "{ \"location\": { \"cityName\": \"Santiago\", \"latitude\": -33.4489, \"longitude\": -70.6693, \"countryName\": \"Chile\" }, \"instantWeather\": { \"airTemperature\": 15.0, \"relativeHumidity\": 70.0, \"airPressureAtSeaLevel\": 1012.5, \"windSpeed\": 5.0, \"cloudAreaFraction\": 50.0 }, \"hourlyForecasts\": [ { \"time\": \"2025-01-01T10:00:00Z\", \"airTemperature\": 16.0, \"windSpeed\": 4.5, \"precipitationAmount\": 0.0 }, { \"time\": \"2025-01-01T11:00:00Z\", \"airTemperature\": 17.0, \"windSpeed\": 4.0, \"precipitationAmount\": 0.0 } ], \"windMap\": { \"type\": \"FeatureCollection\", \"features\": [ { \"type\": \"Feature\", \"geometry\": { \"type\": \"Point\", \"coordinates\": [ -70.6693, -33.4489 ] }, \"properties\": { \"windSpeed\": 5.0, \"windDirection\": 270.0, \"time\": \"2025-01-01T09:00:00Z\" } } ] }, \"airQuality\": { \"latitude\": -33.4489, \"longitude\": -70.6693, \"aqi\": 2, \"components\": { \"co\": 200.0, \"no\": 0.5, \"no2\": 10.0, \"o3\": 40.0, \"so2\": 2.0, \"pm2_5\": 15.0, \"pm10\": 25.0, \"nh3\": 0.1 }, \"timestamp\": 1678886400, \"aqiCategory\": \"Fair\" } }"))),
            @ApiResponse(responseCode = "400", description = "Parámetros de entrada inválidos o ubicación no encontrada",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = com.back.tfm.weatherapp.model.ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = com.back.tfm.weatherapp.model.ErrorResponse.class)))
    })
    @GetMapping("/full-report") // <-- Corregido: Vuelve a ser /full-report
    public Mono<ResponseEntity<com.back.tfm.weatherapp.dto.WeatherResponse>> getFullWeatherReport( // <-- Corregido: Nombre del método
                                                                                                   @Parameter(description = "Nombre de la ciudad", required = true, example = "Santiago") @RequestParam String city,
                                                                                                   @Parameter(description = "Nombre del país", required = true, example = "Chile") @RequestParam String country) {

        System.out.println("--- [Controller] Recibida solicitud /full-report para ciudad: " + city + ", país: " + country + " ---");

        if (city == null || city.trim().isEmpty() || country == null || country.trim().isEmpty()) {
            System.err.println("!!! [Controller] Error 400: Ciudad o país no pueden ser vacíos.");
            return Mono.just(ResponseEntity.badRequest().<com.back.tfm.weatherapp.dto.WeatherResponse>build()); // Asegura el tipo
        }

        System.out.println(">>> [Controller] Llamando a GeocodingService.getCoordinates para obtener lat/lon...");
        return geocodingService.getCoordinates(city, country)
                .flatMap(coords -> {
                    System.out.println("<<< [Controller] Coordenadas obtenidas: " + coords.getLatitude() + ", " + coords.getLongitude());
                    System.out.println(">>> [Controller] Llamando a WeatherService.getAllWeatherData...");
                    return weatherService.getAllWeatherData(
                                    coords.getLatitude(),
                                    coords.getLongitude(),
                                    city,
                                    country
                            )
                            .map(weatherResponse -> {
                                System.out.println("<<< [Controller] WeatherService.getAllWeatherData completado exitosamente.");
                                return ResponseEntity.ok(weatherResponse);
                            })
                            .onErrorResume(e -> {
                                System.err.println("!!! [Controller] Error en WeatherService.getAllWeatherData: " + e.getMessage());
                                e.printStackTrace();
                                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).<com.back.tfm.weatherapp.dto.WeatherResponse>build());
                            });
                })
                .onErrorResume(IllegalArgumentException.class, e -> {
                    System.err.println("!!! [Controller] Error 400: Ubicación no encontrada por GeocodingService. Mensaje: " + e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).<com.back.tfm.weatherapp.dto.WeatherResponse>build());
                })
                .onErrorResume(e -> {
                    System.err.println("!!! [Controller] Error 500: Fallo inesperado en /full-report. Mensaje: " + e.getMessage());
                    e.printStackTrace();
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).<com.back.tfm.weatherapp.dto.WeatherResponse>build());
                })
                .doFinally(signalType -> {
                    System.out.println("--- [Controller] Solicitud /full-report finalizada con estado: " + signalType + " ---");
                });
    }

    @Operation(summary = "Obtiene el meteograma de Yr.no como imagen SVG.",
            description = "Devuelve un archivo SVG que representa el meteograma de pronóstico.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Meteograma obtenido exitosamente",
                    content = @Content(mediaType = "image/svg+xml")),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor al obtener el meteograma")
    })
    @GetMapping("/meteogram")
    public Mono<ResponseEntity<byte[]>> getMeteogram() {
        System.out.println("--- [Controller] Recibida solicitud /meteogram ---");
        return weatherService.getMeteogramAsBytes()
                .map(svgBytes -> {
                    HttpHeaders headers = new HttpHeaders();
                    headers.add(HttpHeaders.CONTENT_TYPE, "image/svg+xml");
                    return new ResponseEntity<>(svgBytes, headers, HttpStatus.OK);
                })
                .doOnError(e -> System.err.println("!!! [Controller] Error al obtener el meteograma: " + e.getMessage()))
                .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    @Operation(summary = "Obtiene las coordenadas geográficas para una ciudad y país dados.",
            description = "Usa un servicio de geocodificación para convertir un nombre de ciudad y país en latitud y longitud.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Coordenadas obtenidas exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LocationCoordinates.class),
                            examples = @ExampleObject(value = "{ \"cityName\": \"Santiago\", \"latitude\": -33.4489, \"longitude\": -70.6693, \"countryName\": \"Chile\" }"))),
            @ApiResponse(responseCode = "400", description = "Parámetros de entrada inválidos o ubicación no encontrada",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = com.back.tfm.weatherapp.model.ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = com.back.tfm.weatherapp.model.ErrorResponse.class)))
    })
    @GetMapping("/location")
    public Mono<ResponseEntity<LocationCoordinates>> getLocation(
            @Parameter(description = "Nombre de la ciudad", required = true, example = "Santiago") @RequestParam String city,
            @Parameter(description = "Nombre del país", required = true, example = "Chile") @RequestParam String country) {

        System.out.println("--- [Controller] Recibida solicitud /location para ciudad: " + city + ", país: " + country + " ---");

        if (city == null || city.trim().isEmpty() || country == null || country.trim().isEmpty()) {
            System.err.println("!!! [Controller] Error 400: Ciudad o país no pueden ser vacíos.");
            return Mono.just(ResponseEntity.badRequest().build());
        }

        System.out.println(">>> [Controller] Llamando a GeocodingService.getCoordinates...");
        return geocodingService.getCoordinates(city, country)
                .map(coords -> {
                    System.out.println("<<< [Controller] GeocodingService completado exitosamente. Coordenadas obtenidas: " + coords);
                    return ResponseEntity.ok(coords);
                })
                .onErrorResume(IllegalArgumentException.class, e -> {
                    System.err.println("<<< [Controller] Error 400: Ubicación no encontrada por GeocodingService. Mensaje: " + e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
                })
                .onErrorResume(e -> {
                    System.err.println("<<< [Controller] Error 500: Fallo inesperado en /location. Mensaje: " + e.getMessage());
                    e.printStackTrace();
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                })
                .doFinally(signalType -> {
                    System.out.println("--- [Controller] Solicitud /location finalizada con estado: " + signalType + " ---");
                });
    }
}