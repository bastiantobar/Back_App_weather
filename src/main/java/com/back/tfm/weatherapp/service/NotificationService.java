package com.back.tfm.weatherapp.service;

import com.back.tfm.weatherapp.model.InstantWeather;
import com.back.tfm.weatherapp.model.UserPreferences;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    @Autowired
    private FirebaseMessaging firebaseMessaging;

    public void sendNotification(String title, String body, String topic) {
        try {
            Message message = Message.builder()
                    .putData("title", title) // Título de la notificación
                    .putData("body", body)   // Cuerpo de la notificación
                    .setTopic(topic)         // Tópico al que se envía
                    .build();

            String response = firebaseMessaging.send(message);
            System.out.println("Notificación enviada: " + response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<String> generateAlerts(InstantWeather weather, Map<String, Object> preferences) {
        List<String> alerts = new ArrayList<>();

        // Verifica si las preferencias del usuario permiten recibir alertas para temperatura
        if (preferences.containsKey("temperature") && (boolean) preferences.get("temperature")) {
            if (weather.getAirTemperature() < 0) {
                alerts.add("¡Alerta! Ola de frío, temperatura bajo cero.");
            } else if (weather.getAirTemperature() > 35) {
                alerts.add("¡Alerta! Ola de calor, temperatura superior a 35°C.");
            }
        }

        // Verifica si las preferencias del usuario permiten recibir alertas para humedad
        if (preferences.containsKey("humidity") && (boolean) preferences.get("humidity")) {
            if (weather.getRelativeHumidity() < 20) {
                alerts.add("¡Alerta! Baja humedad relativa, riesgo de sequedad.");
            }
        }

        // Verifica si las preferencias del usuario permiten recibir alertas para viento
        if (preferences.containsKey("wind") && (boolean) preferences.get("wind")) {
            if (weather.getWindSpeed() > 15) {
                alerts.add("¡Alerta! Fuertes vientos detectados, velocidad superior a 15 m/s.");
            }
        }
        return alerts;
    }


}
