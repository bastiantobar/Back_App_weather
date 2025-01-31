package com.back.tfm.weatherapp.service;

import com.back.tfm.weatherapp.model.InstantWeather;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        // No es necesario inicializar mocks, @ExtendWith(MockitoExtension.class) se encarga
    }

    @Test
    void testSendNotification() throws Exception {
        String title = "Alerta Meteorológica";
        String body = "Fuertes vientos en la región.";
        String topic = "weather_alerts";

        // Mockear el comportamiento de FirebaseMessaging
        when(firebaseMessaging.send(any(Message.class))).thenReturn("mocked_message_id");

        // Ejecutar el método a probar
        notificationService.sendNotification(title, body, topic);

        // Verificar que se llamó a send() con cualquier Message
        verify(firebaseMessaging, times(1)).send(any(Message.class));
    }

    @Test
    void testSendNotificationException() throws Exception {
        doThrow(new RuntimeException("Firebase error"))
                .when(firebaseMessaging).send(any(Message.class));

        assertDoesNotThrow(() -> notificationService.sendNotification("Title", "Body", "Topic"));

        verify(firebaseMessaging, times(1)).send(any(Message.class));
    }

    @Test
    void testGenerateAlerts() {
        InstantWeather weather = new InstantWeather();
        weather.setAirTemperature(-5);
        weather.setRelativeHumidity(15);
        weather.setWindSpeed(20);
        weather.setCloudAreaFraction(90);

        List<String> alerts = notificationService.generateAlerts(weather);

        assertNotNull(alerts);
        assertEquals(4, alerts.size());
        assertTrue(alerts.contains("¡Alerta! Ola de frío, temperatura bajo cero."));
        assertTrue(alerts.contains("¡Alerta! Baja humedad relativa, riesgo de sequedad."));
        assertTrue(alerts.contains("¡Alerta! Fuertes vientos detectados, velocidad superior a 15 m/s."));
        assertTrue(alerts.contains("¡Alerta! Cielo nublado, posibilidad de tormentas."));
    }

    @Test
    void testGenerateAlertsNoAlerts() {
        InstantWeather weather = new InstantWeather();
        weather.setAirTemperature(25);
        weather.setRelativeHumidity(50);
        weather.setWindSpeed(5);
        weather.setCloudAreaFraction(30);

        List<String> alerts = notificationService.generateAlerts(weather);

        assertNotNull(alerts);
        assertTrue(alerts.isEmpty());
    }
}
