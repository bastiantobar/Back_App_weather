package com.back.tfm.weatherapp.controller;

import com.back.tfm.weatherapp.service.FirebaseRealtimeService;
import com.back.tfm.weatherapp.service.NotificationService;
import com.back.tfm.weatherapp.model.InstantWeather;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertControllerTest {

    @Mock
    private FirebaseRealtimeService firebaseRealtimeService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AlertController alertController;

    private final String userId = "testUser123";

    @BeforeEach
    void setUp() {
        // Resetear mocks antes de cada prueba para evitar estados compartidos
        reset(firebaseRealtimeService, notificationService);
    }

    @Test
    void shouldReturnAlertsWhenNotificationsAreEnabled() {
        // Simulación de que el usuario tiene notificaciones activadas
        when(firebaseRealtimeService.getUserNotificationPreference(userId)).thenReturn(Mono.just(true));

        // Simulación del último dato de clima
        InstantWeather mockWeather = new InstantWeather();
        when(firebaseRealtimeService.getLastInstantWeather()).thenReturn(Mono.just(mockWeather));

        // Simulación de alertas generadas
        List<String> mockAlerts = List.of("Tormenta fuerte en la zona", "Riesgo de granizo");
        when(notificationService.generateAlerts(mockWeather)).thenReturn(mockAlerts);

        // Simulación de envío de notificación
        doNothing().when(notificationService).sendNotification(anyString(), anyString(), anyString());

        // Ejecutar el método
        Mono<ResponseEntity<List<String>>> responseMono = alertController.getWeatherAlerts(userId);

        // Verificar la respuesta con StepVerifier
        StepVerifier.create(responseMono)
                .expectNextMatches(response ->
                        response.getStatusCode() == HttpStatus.OK &&
                                response.getBody() != null &&
                                response.getBody().contains("Tormenta fuerte en la zona"))
                .verifyComplete();

        // Verificar que los métodos se llamaron correctamente
        verify(firebaseRealtimeService, times(1)).getUserNotificationPreference(userId);
        verify(firebaseRealtimeService, times(1)).getLastInstantWeather();
        verify(notificationService, times(1)).generateAlerts(mockWeather);
        verify(notificationService, times(1)).sendNotification(anyString(), anyString(), anyString());
    }

    @Test
    void shouldReturnForbiddenWhenNotificationsAreDisabled() {
        // Simulación de que el usuario tiene notificaciones desactivadas
        when(firebaseRealtimeService.getUserNotificationPreference(userId)).thenReturn(Mono.just(false));

        // Ejecutar el método
        Mono<ResponseEntity<List<String>>> responseMono = alertController.getWeatherAlerts(userId);

        // Verificar la respuesta
        StepVerifier.create(responseMono)
                .expectNextMatches(response ->
                        response.getStatusCode() == HttpStatus.FORBIDDEN &&
                                response.getBody() != null &&
                                response.getBody().contains("Notificaciones desactivadas"))
                .verifyComplete();

        // Verificar que no se llamaron otros métodos
        verify(firebaseRealtimeService, times(1)).getUserNotificationPreference(userId);
        verify(firebaseRealtimeService, never()).getLastInstantWeather();
        verify(notificationService, never()).generateAlerts(any());
        verify(notificationService, never()).sendNotification(anyString(), anyString(), anyString());
    }

    @Test
    void shouldReturnInternalServerErrorOnFirebaseError() {
        // Simular error en la consulta a Firebase
        when(firebaseRealtimeService.getUserNotificationPreference(userId)).thenReturn(Mono.error(new RuntimeException("Firebase error")));

        // Ejecutar el método
        Mono<ResponseEntity<List<String>>> responseMono = alertController.getWeatherAlerts(userId);

        // Verificar la respuesta
        StepVerifier.create(responseMono)
                .expectNextMatches(response -> response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR)
                .verifyComplete();

        // Verificar que se intentó obtener la preferencia del usuario
        verify(firebaseRealtimeService, times(1)).getUserNotificationPreference(userId);
        verify(firebaseRealtimeService, never()).getLastInstantWeather();
        verify(notificationService, never()).generateAlerts(any());
        verify(notificationService, never()).sendNotification(anyString(), anyString(), anyString());
    }
}
