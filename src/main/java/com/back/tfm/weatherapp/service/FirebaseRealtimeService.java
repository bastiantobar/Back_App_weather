package com.back.tfm.weatherapp.service;

import com.back.tfm.weatherapp.model.HourlyForecast;
import com.back.tfm.weatherapp.model.InstantWeather;
import com.back.tfm.weatherapp.model.WindMap;
import com.back.tfm.weatherapp.model.WindMapPoint;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DatabaseError;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature; // ¡Importa esto!
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule; // ¡Importa esto para tipos de fecha Java 8!

@Service
public class FirebaseRealtimeService {

    private final DatabaseReference databaseReference;
    private final ObjectMapper objectMapper; // Solo un ObjectMapper

    public FirebaseRealtimeService(DatabaseReference databaseReference) {
        this.databaseReference = databaseReference;
        this.objectMapper = new ObjectMapper();
        // ¡Configura el ObjectMapper aquí! ESTO ES CRUCIAL
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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
                                // Asegúrate de que `getTime()` devuelve un formato ISO-8601 si es un String
                                try {
                                    Instant forecastTime = Instant.parse(forecast.getTime());
                                    return forecastTime.isAfter(last48Hours);
                                } catch (java.time.format.DateTimeParseException e) {
                                    System.err.println("Error parsing forecast time: " + forecast.getTime() + " - " + e.getMessage());
                                    return false; // Filtra los que no se puedan parsear
                                }
                            })
                            .collect(Collectors.toList());
                });
    }
    public Mono<InstantWeather> getLastInstantWeather() {
        // CORRECCIÓN: Apuntar al nodo correcto si InstantWeather se guarda allí
        return fetchFromFirebase("InstantWeather", InstantWeather.class)
                .flatMap(list -> {
                    if (list == null || list.isEmpty()) { // Añadir null check para la lista
                        return Mono.empty();
                    }
                    // Asumiendo que el último elemento es el más reciente o el que deseas.
                    // Podrías necesitar un ordenamiento por tiempo si no lo garantizan.
                    return Mono.just(list.get(list.size() - 1));
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
                            // Usamos el 'objectMapper' configurado para la deserialización
                            WindMap fullMap = objectMapper.convertValue(childSnapshot.getValue(), WindMap.class);
                            if (fullMap != null && fullMap.getFeatures() != null) {
                                List<WindMapPoint> features = fullMap.getFeatures();
                                WindMapPoint latestFeature = features.stream()
                                        .max(Comparator.comparing(f -> {
                                            try {
                                                return Instant.parse(f.getProperties().getTime());
                                            } catch (java.time.format.DateTimeParseException e) {
                                                System.err.println("Error parsing WindMapPoint time: " + f.getProperties().getTime() + " - " + e.getMessage());
                                                return Instant.EPOCH; // Retorna un Instant base para no fallar el comparator
                                            }
                                        }))
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
                    try {
                        // ¡Usa nuestro ObjectMapper configurado para cada elemento de la lista!
                        T value = objectMapper.convertValue(childSnapshot.getValue(), clazz);
                        if (value != null) {
                            data.add(value);
                        }
                    } catch (Exception e) {
                        System.err.println("!!! [FirebaseRealtimeService] Error convirtiendo datos para " + node + " (tipo: " + clazz.getSimpleName() + "): " + e.getMessage());
                        // Decide si quieres lanzar una excepción o simplemente omitir el elemento defectuoso
                        // future.completeExceptionally(new RuntimeException("Error al convertir elemento de la lista", e));
                        // return; // Salir si hay un error para evitar añadir datos incorrectos
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

    /**
     * Intenta obtener datos de cualquier tipo desde el caché de Firebase Realtime Database.
     * @param key La clave del caché.
     * @param clazz La clase del tipo de datos a recuperar (ej. LocationCoordinates.class, AirQuality.class).
     * @param <T> El tipo de datos a recuperar.
     * @return Mono que emite el objeto cacheado o null si no se encuentra o hay un error.
     */
    // Método getGeoCache genérico
    public <T> Mono<T> getGeoCache(String key, Class<T> type) {
        return Mono.create(sink -> {
            databaseReference.child("geocaching_cache").child(key) // Ruta consistente para todos los datos geográficos en caché
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                try {
                                    // Usar ObjectMapper para convertir el Map genérico de Firebase al DTO deseado
                                    T cachedObject = objectMapper.convertValue(dataSnapshot.getValue(), type);
                                    System.out.println("--- [FirebaseRealtimeService] Cache hit for " + key + ", type: " + type.getSimpleName());
                                    sink.success(cachedObject);
                                } catch (Exception e) {
                                    System.err.println("!!! [FirebaseRealtimeService] Error converting cached data for " + key + " (type: " + type.getSimpleName() + "): " + e.getMessage());
                                    sink.error(new RuntimeException("Error converting cached data", e));
                                }
                            } else {
                                System.out.println("--- [FirebaseRealtimeService] Cache miss for " + key);
                                sink.success(null); // Indica que no se encontraron datos
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            System.err.println("!!! [FirebaseRealtimeService] Error fetching geocaching cache from Firebase for " + key + ": " + databaseError.getMessage());
                            sink.error(new RuntimeException("Error al leer caché de geocodificación", databaseError.toException()));
                        }
                    });
        })
                ; // Cierra el Mono.create, eliminando la línea .flatMap
    }

    // Método saveGeoCache genérico
    public Mono<Void> saveGeoCache(String key, Object data) {
        return Mono.fromFuture(
                CompletableFuture.runAsync(() -> {
                    try {
                        // Firebase Realtime Database SDK usa su propio ObjectMapper para setValueAsync.
                        // Si 'data' contiene tipos como OffsetDateTime, la clave es que el DTO
                        // tenga los `@JsonProperty` y Lombok `@NoArgsConstructor`/`@AllArgsConstructor`
                        // correctos, y que Firebase internamente maneje bien la serialización.
                        databaseReference.child("geocaching_cache").child(key).setValueAsync(data); // Ruta consistente
                        System.out.println("--- [FirebaseRealtimeService] Saved data to geocaching_cache for key: " + key);
                    } catch (Exception e) {
                        System.err.println("!!! [FirebaseRealtimeService] Error saving geocaching cache to Firebase for key: " + key + ": " + e.getMessage());
                        throw new RuntimeException("Failed to save geocaching cache", e);
                    }
                })
        );
    }
}