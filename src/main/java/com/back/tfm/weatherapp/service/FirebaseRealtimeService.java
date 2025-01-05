package com.back.tfm.weatherapp.service;

import com.back.tfm.weatherapp.model.HourlyForecast;
import com.back.tfm.weatherapp.model.InstantWeather;
import com.back.tfm.weatherapp.model.WindMap;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DatabaseError;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class FirebaseRealtimeService {

    private final DatabaseReference databaseReference;

    public FirebaseRealtimeService(DatabaseReference databaseReference) {
        this.databaseReference = databaseReference;
    }

    // Persistir clima instantáneo
    public void saveInstantWeather(InstantWeather weather) {
        String key = databaseReference.child("InstantWeather").push().getKey();
        databaseReference.child("InstantWeather").child(key).setValueAsync(weather);
    }

    // Persistir pronósticos horarios
    public void saveHourlyForecasts(List<HourlyForecast> forecasts) {
        DatabaseReference ref = databaseReference.child("HourlyForecasts");
        for (HourlyForecast forecast : forecasts) {
            String key = ref.push().getKey();
            ref.child(key).setValueAsync(forecast);
        }
    }

    // Persistir mapas de viento
    public void saveWindMap(WindMap windMap) {
        String key = databaseReference.child("WindMaps").push().getKey();
        databaseReference.child("WindMaps").child(key).setValueAsync(windMap);
    }

    // Leer datos de clima instantáneo persistidos
    public Mono<List<InstantWeather>> getAllInstantWeather() {
        return fetchFromFirebase("InstantWeather", InstantWeather.class);
    }

    // Leer pronósticos horarios persistidos
    public Mono<List<HourlyForecast>> getAllHourlyForecasts() {
        return fetchFromFirebase("HourlyForecasts", HourlyForecast.class);
    }

    // Leer mapas de viento persistidos
    public Mono<List<WindMap>> getAllWindMaps() {
        return fetchFromFirebase("WindMaps", WindMap.class);
    }

    // Método genérico para leer datos desde Firebase
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
}
