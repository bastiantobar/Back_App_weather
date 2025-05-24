package com.back.tfm.weatherapp.service;

import com.back.tfm.weatherapp.dto.LocationCoordinates;
import com.back.tfm.weatherapp.model.HourlyForecast;
import com.back.tfm.weatherapp.model.InstantWeather;
import com.back.tfm.weatherapp.model.WindMap;
import com.back.tfm.weatherapp.model.WindMapPoint;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DatabaseError;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class FirebaseRealtimeService {

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
    // Método genérico para leer datos desde Firebase
    protected  <T> Mono<List<T>> fetchFromFirebase(String node, Class<T> clazz) {
        CompletableFuture<List<T>> future = new CompletableFuture<>();
        databaseReference.child(node).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<T> data = new ArrayList<>();
                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    T value = childSnapshot.getValue(clazz);
                    if (value != null) { // Añadir verificación de nulos al obtener el valor
                        data.add(value);
                    }
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
    public Mono<Boolean> getUserNotificationPreference(String userId) {
        return Mono.create(sink -> {
            databaseReference.child("users").child(userId).child("notifications_enabled")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            Boolean notificationsEnabled = dataSnapshot.getValue(Boolean.class);
                            sink.success(notificationsEnabled != null ? notificationsEnabled : false);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            sink.error(new Exception("Error al obtener la preferencia de notificaciones: " + databaseError.getMessage()));
                        }
                    });
        });
    }


    public <T> Mono<T> getGeoCache(String key, Class<T> clazz) {
        return Mono.create(sink -> {
            System.out.println("--- [FirebaseRealtimeService] Iniciando getGeoCache para clave: " + key + " en el Mono.create.");
            databaseReference.child("geocoding_cache").child(key)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            System.out.println("--- [FirebaseRealtimeService] Callback onDataChange disparado para: " + key);
                            if (dataSnapshot.exists()) {
                                try {
                                    T cachedData = dataSnapshot.getValue(clazz); // <-- ¡Aquí es donde creemos que falla!
                                    System.out.println(">>> [FirebaseRealtimeService] Cache hit for " + key + ". Data: " + cachedData);
                                    sink.success(cachedData);
                                } catch (Exception e) {
                                    System.err.println("!!! [FirebaseRealtimeService] ERROR al parsear datos de caché para " + key + ": " + e.getMessage());
                                    e.printStackTrace();
                                    sink.error(new RuntimeException("Error al parsear datos de caché de Firebase para " + key, e));
                                }
                            } else {
                                System.out.println(">>> [FirebaseRealtimeService] Cache miss for " + key);
                                sink.success(null);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            System.err.println("--- [FirebaseRealtimeService] Callback onCancelled disparado para: " + key);
                            System.err.println("!!! [FirebaseRealtimeService] Error fetching geocoding cache from Firebase for " + key + ": " + databaseError.getMessage());
                            sink.error(new RuntimeException("Error al leer caché de geocodificación", databaseError.toException()));
                        }
                    });
        });
    }

    public Mono<Void> saveGeoCache(String key, LocationCoordinates data) {
        return Mono.fromFuture(
                CompletableFuture.runAsync(() -> {
                    try {
                        databaseReference.child("geocoding_cache").child(key).setValueAsync(data);
                    } catch (Exception e) {
                        System.err.println("!!! [FirebaseRealtimeService] Error saving geocoding cache to Firebase: " + e.getMessage());
                        throw new RuntimeException("Failed to save geocoding cache", e);
                    }
                })
        );
    }

}