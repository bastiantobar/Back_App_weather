
package com.back.tfm.weatherapp.controller;

import com.back.tfm.weatherapp.service.FirebaseRealtimeService;
import com.back.tfm.weatherapp.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AlertController {

    private final FirebaseRealtimeService firebaseRealtimeService;
    private final NotificationService notificationService;

    @Autowired
    public AlertController(FirebaseRealtimeService firebaseRealtimeService, NotificationService notificationService) {
        this.firebaseRealtimeService = firebaseRealtimeService;
        this.notificationService = notificationService;
    }

    @Operation(
            summary = "Generar alertas meteorológicas basadas en el último dato de clima",
            description = "Verifica si el usuario tiene activadas las notificaciones antes de enviar alertas."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alertas generadas exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "403", description = "Notificaciones desactivadas para el usuario",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/alerts/{userId}")
    public Mono<ResponseEntity<List<String>>> getWeatherAlerts(@PathVariable String userId) {
        return firebaseRealtimeService.getUserNotificationPreference(userId) // ✅ Consultamos si el usuario tiene activadas las notificaciones
                .flatMap(notificationsEnabled -> {
                    if (!notificationsEnabled) {
                        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(List.of("Notificaciones desactivadas")));
                    }

                    return firebaseRealtimeService.getLastInstantWeather()
                            .map(weather -> {
                                List<String> alerts = notificationService.generateAlerts(weather);

                                if (!alerts.isEmpty()) {
                                    String title = "⚠️ Alerta Meteorológica";
                                    String body = String.join(", ", alerts);
                                    notificationService.sendNotification(title, body, "weather_alerts");
                                }

                                return ResponseEntity.ok(alerts);
                            });
                })
                .onErrorResume(e -> {
                    e.printStackTrace();
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }
}
