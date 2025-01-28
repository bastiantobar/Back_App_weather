package com.back.tfm.weatherapp.controller;

import com.back.tfm.weatherapp.model.InstantWeather;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
            description = "Devuelve una lista de alertas meteorológicas según los datos del clima obtenidos."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alertas generadas exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/alerts")
    public Mono<ResponseEntity<List<String>>> getWeatherAlerts() {
        return firebaseRealtimeService.getLastInstantWeather()
                .map(weather -> {
                    List<String> alerts = notificationService.generateAlerts(weather);

                    if (!alerts.isEmpty()) {
                        String title = "Alerta Meteorológica";
                        String body = String.join(", ", alerts);
                        notificationService.sendNotification(title, body, "weather_alerts");
                    }

                    return ResponseEntity.ok(alerts);
                })
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()));
    }
}
