package com.back.tfm.weatherapp.service;

import com.back.tfm.weatherapp.controller.AlertController;
import com.back.tfm.weatherapp.model.*;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DatabaseError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class FirebaseRealtimeService {
    private static final Logger logger = LoggerFactory.getLogger(FirebaseRealtimeService.class);

    private final DatabaseReference databaseReference;

    public FirebaseRealtimeService(DatabaseReference databaseReference) {
        this.databaseReference = databaseReference;
    }

    public void saveInstantWeather(InstantWeather weather) {
        String key = databaseReference.child("InstantWeather").push().getKey();
        databaseReference.child("InstantWeather").child(key).setValueAsync(weather);
    }
    public void saveHourlyForecasts(List<HourlyForecast> forecasts) {
        DatabaseReference ref = databaseReference.child("HourlyForecasts");
        for (HourlyForecast forecast : forecasts) {
            String key = ref.push().getKey();
            ref.child(key).setValueAsync(forecast);
        }
    }
    public void saveWindMap(WindMap windMap) {
        String key = databaseReference.child("WindMaps").push().getKey();
        databaseReference.child("WindMaps").child(key).setValueAsync(windMap);
    }
    public Mono<List<InstantWeather>> getAllInstantWeather() {
        return fetchFromFirebase("InstantWeather", InstantWeather.class);
    }
    public Mono<List<HourlyForecast>> getAllHourlyForecasts() {
        return fetchFromFirebase("HourlyForecasts", HourlyForecast.class);
    }
    public Mono<List<HourlyForecast>> getHourlyForecastsLast48Hours() {
        return getAllHourlyForecasts() // Obtener todos los pronósticos
                .map(hourlyForecasts -> {
                    Instant now = Instant.now();
                    Instant last48Hours = now.minusSeconds(48 * 60 * 60); // Restar 48 horas

                    return hourlyForecasts.stream()
                            .filter(forecast -> {
                                Instant forecastTime = Instant.parse(forecast.getTime()); // Asegúrate de que `getTime` sea un formato ISO-8601
                                return forecastTime.isAfter(last48Hours);
                            })
                            .collect(Collectors.toList());
                });
    }
    public Mono<InstantWeather> getLastInstantWeather() {
        return fetchFromFirebase("HourlyForecasts", InstantWeather.class)
                .flatMap(list -> {
                    if (list.isEmpty()) {
                        return Mono.empty();
                    }
                    return Mono.just(list.get(list.size() - 1)); // Obtener el último registro
                });
    }
    public Mono<WindMap> getLastWindMap() {
        CompletableFuture<WindMap> future = new CompletableFuture<>();
        databaseReference.child("WindMaps")
                .orderByKey()
                .limitToLast(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                            WindMap fullMap = childSnapshot.getValue(WindMap.class);
                            if (fullMap != null && fullMap.getFeatures() != null) {
                                List<WindMapPoint> features = fullMap.getFeatures();
                                WindMapPoint latestFeature = features.stream()
                                        .max(Comparator.comparing(f -> f.getProperties().getTime()))
                                        .orElse(null);

                                if (latestFeature != null) {
                                    WindMap result = new WindMap();
                                    result.setType("FeatureCollection");
                                    result.setFeatures(List.of(latestFeature));
                                    future.complete(result);
                                    return;
                                }
                            }
                        }
                        future.complete(null); // Si no hay datos, completar con null
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        future.completeExceptionally(new RuntimeException("Error al leer WindMap: " + error.getMessage()));
                    }
                });
        return Mono.fromFuture(future);
    }
    public Mono<List<WindMap>> getAllWindMaps() {
        return fetchFromFirebase("WindMaps", WindMap.class);
    }
    private <T> Mono<List<T>> fetchFromFirebase(String node, Class<T> clazz) {
        CompletableFuture<List<T>> future = new CompletableFuture<>();
        databaseReference.child(node).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<T> data = new ArrayList<>();
                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    T value = childSnapshot.getValue(clazz);
                    data.add(value);
                }
                future.complete(data);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                future.completeExceptionally(new RuntimeException("Error al leer datos de Firebase: " + error.getMessage()));
            }
        });
        return Mono.fromFuture(future);
    }
    public void saveUserPreferences(String userId, String email, UserPreferences preferences) {
        DatabaseReference userRef = databaseReference.child("PreferencesUsers").child(userId);

        // Crear un nuevo ID único para cada preferencia
        String preferencesId = userRef.push().getKey();  // Genera un ID único

        // Crear un mapa con los datos a guardar
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);
        userData.put("preferences", preferences);

        // Usar el nuevo ID para crear un nuevo registro
        userRef.child(preferencesId).setValue(userData, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                if (databaseError != null) {
                    System.err.println("Error al guardar preferencias: " + databaseError.getMessage());
                } else {
                    System.out.println("Preferencias guardadas exitosamente");
                }
            }
        });
    }
    public Mono<Map<String, Object>> getUserPreferences(String userId) {
        logger.debug("Iniciando la obtención de preferencias para el usuario con userId={}", userId);

        DatabaseReference userRef = databaseReference.child("PreferencesUsers").child(userId);
        logger.error("Preferencias obtenidas userRef: {}", userRef);
        return Mono.create(sink -> {
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        logger.error("Preferencias encontradas para el usuario con userId={}", userId);
                        Map<String, Object> preferences = (Map<String, Object>) snapshot.getValue(); // Generalizamos para aceptar cualquier tipo de dato
                        logger.error("Preferencias obtenidas: {}", preferences);
                        sink.success(preferences); // Devolver las preferencias como un Map
                    } else {
                        logger.warn("No se encontraron preferencias para el usuario con userId={}", userId);
                        sink.error(new RuntimeException("No preferences found for user"));
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    logger.error("Error al leer preferencias del usuario con userId={}: {}", userId, error.getMessage());
                    sink.error(new RuntimeException("Error al leer preferencias: " + error.getMessage()));
                }
            });
        });
    }

}
