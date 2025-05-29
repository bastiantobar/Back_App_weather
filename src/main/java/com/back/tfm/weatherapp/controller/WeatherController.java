// src/main/java/com/back/tfm/weatherapp/controller/WeatherController.java
package com.back.tfm.weatherapp.controller;

import com.back.tfm.weatherapp.dto.LocationCoordinates;
import com.back.tfm.weatherapp.dto.WeatherResponse; // Importa tu DTO de respuesta consolidada
import com.back.tfm.weatherapp.model.*;
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
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;
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
    // Método para obtener el reporte meteorológico completo por una cadena de dirección
    @GetMapping("/full-report")
    public Mono<ResponseEntity<WeatherResponse>> getFullWeatherReport(
            @Parameter(description = "Cadena de búsqueda de la dirección (ej. 'Calle Falsa 123, Springfield, USA' o 'Hijuelas, Valparaiso, Chile')", required = true, example = "Hijuelas, Valparaiso, Chile")
            @RequestParam String addressQuery) {

        System.out.println("--- [Controller] Recibida solicitud /full-report para dirección: " + addressQuery + " ---");

        if (addressQuery == null || addressQuery.trim().isEmpty()) {
            System.err.println("!!! [Controller] Error 400: La cadena de dirección no puede ser vacía.");
            return Mono.just(ResponseEntity.badRequest().<WeatherResponse>build());
        }

        System.out.println(">>> [Controller] Llamando a GeocodingService.getCoordinates para obtener lat/lon...");
        // Pasar la cadena de dirección completa al servicio de geocodificación
        return geocodingService.getCoordinates(addressQuery) // ¡CAMBIO: Solo se pasa addressQuery!
                .flatMap(coords -> {
                    System.out.println("<<< [Controller] Coordenadas obtenidas: " + coords.getLatitude() + ", " + coords.getLongitude() + " para " + coords.getName());
                    System.out.println(">>> [Controller] Llamando a WeatherService.getAllWeatherData...");
                    return weatherService.getAllWeatherData(
                                    coords.getLatitude(),
                                    coords.getLongitude(),
                                    coords.getName(), // Usar el nombre de la ubicación devuelto por Nominatim
                                    coords.getCountryCode() // Usar el código de país devuelto por Nominatim
                            )
                            .map(weatherResponse -> {
                                System.out.println("<<< [Controller] WeatherService.getAllWeatherData completado exitosamente.");
                                return ResponseEntity.ok(weatherResponse);
                            })
                            .onErrorResume(e -> {
                                System.err.println("!!! [Controller] Error en WeatherService.getAllWeatherData: " + e.getMessage());
                                e.printStackTrace();
                                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).<WeatherResponse>build());
                            });
                })
                .onErrorResume(IllegalArgumentException.class, e -> {
                    System.err.println("!!! [Controller] Error 400: Ubicación no encontrada por GeocodingService. Mensaje: " + e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).<WeatherResponse>build());
                })
                .onErrorResume(e -> {
                    System.err.println("!!! [Controller] Error 500: Fallo inesperado en /full-report. Mensaje: " + e.getMessage());
                    e.printStackTrace();
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).<WeatherResponse>build());
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
    // Adaptar el endpoint /location también para que use la cadena de búsqueda única
    @GetMapping("/location")
    public Mono<ResponseEntity<LocationCoordinates>> getLocation(
            @Parameter(description = "Cadena de búsqueda de la dirección (ej. 'Santiago, Chile')", required = true, example = "Santiago, Chile")
            @RequestParam String addressQuery) {

        System.out.println("--- [Controller] Recibida solicitud /location para dirección: " + addressQuery + " ---");

        if (addressQuery == null || addressQuery.trim().isEmpty()) {
            System.err.println("!!! [Controller] Error 400: La cadena de dirección no puede ser vacía.");
            return Mono.just(ResponseEntity.badRequest().<LocationCoordinates>build());
        }

        System.out.println(">>> [Controller] Llamando a GeocodingService.getCoordinates...");
        return geocodingService.getCoordinates(addressQuery) // ¡CAMBIO: Solo se pasa addressQuery!
                .map(coords -> {
                    System.out.println("<<< [Controller] GeocodingService completado exitosamente. Coordenadas obtenidas: " + coords);
                    return ResponseEntity.ok(coords);
                })
                .onErrorResume(IllegalArgumentException.class, e -> {
                    System.err.println("<<< [Controller] Error 400: Ubicación no encontrada por GeocodingService. Mensaje: " + e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).<LocationCoordinates>build());
                })
                .onErrorResume(e -> {
                    System.err.println("<<< [Controller] Error 500: Fallo inesperado en /location. Mensaje: " + e.getMessage());
                    e.printStackTrace();
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).<LocationCoordinates>build());
                })
                .doFinally(signalType -> {
                    System.out.println("--- [Controller] Solicitud /location finalizada con estado: " + signalType + " ---");
                });
    }
    @Operation(summary = "Guarda manualmente un registro histórico de datos meteorológicos.",
            description = "Este endpoint es para guardar una entrada histórica específica. En un sistema real, esto podría ser automático.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Objeto HistoricalWeatherEntry a guardar",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = HistoricalWeatherEntry.class)
                    )
            ))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Registro histórico guardado exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class),
                            examples = @ExampleObject(value = "\"Registro histórico con ID: -NsAcX_Y6Z-yE9fC0aBc guardado exitosamente.\""))),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida (ej. datos faltantes)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor al guardar",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/historical/save")
    public Mono<ResponseEntity<String>> saveHistoricalData(@RequestBody HistoricalWeatherEntry historicalEntry) {
        if (historicalEntry == null || historicalEntry.getLocation() == null || historicalEntry.getRecordedAt() == null) {
            return Mono.just(ResponseEntity.badRequest().body("Datos de registro histórico incompletos. Se requiere Location y RecordedAt."));
        }
        System.out.println(">>> [Controller] Solicitud para guardar registro histórico para lat: " + historicalEntry.getLocation().getLatitude() + ", lon: " + historicalEntry.getLocation().getLongitude());

        // Asegurarse de que el recordedAt esté presente, si no, usar el momento actual
        if (historicalEntry.getRecordedAt() == null) {
            historicalEntry.setRecordedAt(Instant.now());
        }

        return firebaseRealtimeService.saveHistoricalWeatherEntry(historicalEntry)
                .map(id -> {
                    System.out.println("<<< [Controller] Registro histórico guardado con ID: " + id);
                    return ResponseEntity.status(HttpStatus.CREATED).body("Registro histórico con ID: " + id + " guardado exitosamente.");
                })
                .onErrorResume(e -> {
                    System.err.println("!!! [Controller] Error al guardar el registro histórico: " + e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al guardar el registro histórico: " + e.getMessage()));
                });
    }

    @Operation(summary = "Obtiene registros históricos de datos meteorológicos para una ubicación.",
            description = "Recupera los registros históricos de datos meteorológicos para una latitud y longitud dadas.",
            parameters = {
                    @Parameter(name = "latitude", description = "Latitud de la ubicación", required = true, example = "-33.4489"),
                    @Parameter(name = "longitude", description = "Longitud de la ubicación", required = true, example = "-70.6693"),
                    @Parameter(name = "limit", description = "Número máximo de registros a recuperar (por defecto: 10)", required = false, example = "5")
            })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registros históricos obtenidos exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(type = "array", implementation = HistoricalWeatherEntry.class))),
            @ApiResponse(responseCode = "400", description = "Parámetros de latitud o longitud inválidos",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor al recuperar registros",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/historical")
    public Mono<ResponseEntity<List<HistoricalWeatherEntry>>> getHistoricalData(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "10") int limit) { // Por defecto, obtener 10 registros

        System.out.println(">>> [Controller] Solicitud de registros históricos para lat: " + latitude + ", lon: " + longitude + " con límite: " + limit);

        return firebaseRealtimeService.getHistoricalWeatherEntries(latitude, longitude, limit)
                .map(entries -> {
                    System.out.println("<<< [Controller] Registros históricos obtenidos: " + entries.size());
                    return ResponseEntity.ok(entries);
                })
                .onErrorResume(e -> {
                    System.err.println("!!! [Controller] Error al obtener registros históricos: " + e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of())); // Retorna lista vacía en caso de error
                });
    }
}