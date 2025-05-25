package com.back.tfm.weatherapp.service;

import com.back.tfm.weatherapp.dto.SunriseSunsetResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class SunriseSunsetService {

    private final WebClient sunriseSunsetWebClient;
    private final FirebaseRealtimeService firebaseRealtimeService;

    // Cache expiration time for sunrise/sunset data (e.g., 24 hours in seconds).
    // The API response doesn't have an "updated_at", so we rely on this.
    // For simplicity, we'll store the `Results` object directly.
    private static final long CACHE_EXPIRATION_SECONDS = 24 * 3600; // 24 hours

    public SunriseSunsetService(@Qualifier("sunriseSunsetWebClient") WebClient sunriseSunsetWebClient,
                                FirebaseRealtimeService firebaseRealtimeService) {
        this.sunriseSunsetWebClient = sunriseSunsetWebClient;
        this.firebaseRealtimeService = firebaseRealtimeService;
    }

    public Mono<SunriseSunsetResponse.Results> getSunriseSunsetTimes(double lat, double lon, LocalDate date) {
        String dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String cacheKey = String.format(Locale.US, "sunrise_sunset_%.4f_%.4f_%s", lat, lon, dateString)
                .replace(".", "_").replace("-", "minus");

        System.out.println(">>> [SunriseSunsetService] Intentando obtener Sunrise/Sunset desde caché para key: " + cacheKey);

        // Intenta obtener de la caché. `getGeoCache` devolverá Mono.empty() si no encuentra o está expirado.
        return firebaseRealtimeService.getGeoCache(cacheKey, SunriseSunsetResponse.Results.class)
                .flatMap(cachedData -> {
                    // Aquí asumimos que getGeoCache o el mecanismo de Firebase ya determinó la validez.
                    // Si cachedData no es null, se considera válido para esta invocación.
                    if (cachedData != null) {
                        System.out.println("<<< [SunriseSunsetService] Sirviendo Sunrise/Sunset desde caché para " + lat + ", " + lon + ", " + dateString + ".");
                        return Mono.just(cachedData);
                    }
                    System.out.println("--- [SunriseSunsetService] Cache miss para Sunrise/Sunset. Pasando a switchIfEmpty.");
                    return Mono.empty(); // Indica caché fallida
                })
                .switchIfEmpty(Mono.defer(() -> {
                    System.out.println(">>> [SunriseSunsetService] switchIfEmpty: No encontrado en caché o expirado. Llamando a Sunrise-Sunset API.");
                    return callSunriseSunsetApi(lat, lon, dateString, cacheKey);
                }))
                .doOnError(e -> System.err.println("!!! [SunriseSunsetService] Error en getSunriseSunsetTimes: " + e.getMessage()));
    }

    private Mono<SunriseSunsetResponse.Results> callSunriseSunsetApi(double lat, double lon, String dateString, String cacheKey) {
        String url = String.format(Locale.US, "json?lat=%.7f&lng=%.7f&date=%s", lat, lon, dateString);
        System.out.println("--- [SunriseSunsetService] Llamando a Sunrise-Sunset API con path: " + url);
        return sunriseSunsetWebClient.get()
                .uri(url)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> {
                    System.err.println("!!! [SunriseSunsetService] Error de HTTP de Sunrise-Sunset API (onStatus): " + response.statusCode());
                    return Mono.error(new RuntimeException("Error de Sunrise-Sunset API: " + response.statusCode()));
                })
                .bodyToMono(SunriseSunsetResponse.class)
                .flatMap(apiResponse -> {
                    if ("OK".equalsIgnoreCase(apiResponse.getStatus()) && apiResponse.getResults() != null) {
                        System.out.println("--- [SunriseSunsetService] Sunrise-Sunset API response recibida y deserializada.");

                        // *** AÑADE ESTA LÍNEA PARA VERIFICAR EL OBJETO COMPLETO ANTES DE CACHE ***
                        System.out.println("--- [SunriseSunsetService] Objeto Results a guardar en caché: " + apiResponse.getResults());
                        // *******************************************************************

                        return firebaseRealtimeService.saveGeoCache(cacheKey, apiResponse.getResults())
                                .thenReturn(apiResponse.getResults());
                    } else {
                        System.err.println("!!! [SunriseSunsetService] Respuesta no OK de Sunrise-Sunset API: " + apiResponse.getStatus());
                        return Mono.error(new RuntimeException("API de Sunrise-Sunset devolvió estado no OK o resultados nulos."));
                    }
                })
                .doOnError(e -> {
                    System.err.println("!!! [SunriseSunsetService] Error en la llamada a Sunrise-Sunset API o en el parseo: " + e.getMessage());
                    e.printStackTrace();
                });
    }
}