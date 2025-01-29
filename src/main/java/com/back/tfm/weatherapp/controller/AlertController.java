package com.back.tfm.weatherapp.controller;

import com.back.tfm.weatherapp.model.InstantWeather;
import com.back.tfm.weatherapp.model.UserPreferences;
import com.back.tfm.weatherapp.service.AuthService;
import com.back.tfm.weatherapp.service.FirebaseRealtimeService;
import com.back.tfm.weatherapp.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AlertController {

    private static final Logger logger = LoggerFactory.getLogger(AlertController.class);

    private final FirebaseRealtimeService firebaseRealtimeService;
    private final AuthService authService;
    private final NotificationService notificationService;

    @Autowired
    public AlertController(FirebaseRealtimeService firebaseRealtimeService, NotificationService notificationService, AuthService authService) {
        this.firebaseRealtimeService = firebaseRealtimeService;
        this.notificationService = notificationService;
        this.authService = authService;
    }

    @Operation(
            summary = "Generar alertas meteorológicas basadas en el último dato de clima",
            description = "Devuelve una lista de alertas meteorológicas según los datos del clima obtenidos."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alertas generadas exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/alerts")
    public Mono<ResponseEntity<List<String>>> getWeatherAlerts(@RequestHeader("Authorization") String authToken) {
        try {
            String userId = authService.getUserIdFromToken(authToken.replace("Bearer ", ""));
            logger.debug("Usuario autenticado con userId={}", userId);

            return getUserPreferences(userId)
                    .flatMap(preferences -> getLastWeatherData()
                            .map(weather -> generateAlerts(weather, preferences))
                            .map(alerts -> {
                                if (!alerts.isEmpty()) {
                                    sendNotification(userId, alerts);
                                }
                                return ResponseEntity.ok(alerts); // Devolver las alertas generadas
                            })
                            .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()))
                    );
        } catch (Exception e) {
            logger.error("Error al procesar el token: {}", e.getMessage());
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
    }

    // Obtener las preferencias del usuario
    private Mono<Map<String, Object>> getUserPreferences(String userId) {
        return firebaseRealtimeService.getUserPreferences(userId)
                .doOnTerminate(() -> logger.debug("Preferencias obtenidas para el usuario con userId={}", userId));
    }

    // Obtener los últimos datos meteorológicos
    private Mono<InstantWeather> getLastWeatherData() {
        return firebaseRealtimeService.getLastInstantWeather()
                .doOnTerminate(() -> logger.debug("Últimos datos meteorológicos obtenidos."));
    }

    // Generar alertas meteorológicas basadas en las preferencias del usuario y los datos meteorológicos
    private List<String> generateAlerts(InstantWeather weather, Map<String, Object> preferences) {
        List<String> alerts = new ArrayList<>();

        // Comprobar preferencias para generar alertas de temperatura
        if (preferences.containsKey("temperature") && (boolean) preferences.get("temperature")) {
            if (weather.getAirTemperature() < 0) {
                alerts.add("¡Alerta! Ola de frío, temperatura bajo cero.");
            } else if (weather.getAirTemperature() > 35) {
                alerts.add("¡Alerta! Ola de calor, temperatura superior a 35°C.");
            }
        }

        // Comprobar preferencias para generar alertas de humedad
        if (preferences.containsKey("humidity") && (boolean) preferences.get("humidity")) {
            if (weather.getRelativeHumidity() < 20) {
                alerts.add("¡Alerta! Baja humedad relativa, riesgo de sequedad.");
            }
        }

        // Comprobar preferencias para generar alertas de viento
        if (preferences.containsKey("wind") && (boolean) preferences.get("wind")) {
            if (weather.getWindSpeed() > 15) {
                alerts.add("¡Alerta! Fuertes vientos detectados, velocidad superior a 15 m/s.");
            }
        }

        // Agregar una alerta genérica para probar el flujo
        alerts.add("¡Alerta! Esta es una prueba de alerta generada.");

        return alerts;
    }


    // Enviar notificación de alerta al usuario
    private void sendNotification(String userId, List<String> alerts) {
        String title = "Alerta Meteorológica";
        String body = String.join(", ", alerts);
        notificationService.sendNotification(title, body, userId);
        logger.debug("Notificación enviada al usuario {}", userId);
    }




}
