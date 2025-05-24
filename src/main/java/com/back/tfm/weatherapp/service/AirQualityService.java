package com.back.tfm.weatherapp.service;

import com.back.tfm.weatherapp.dto.AirQuality;
import com.back.tfm.weatherapp.dto.OpenWeatherAirPollutionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AirQualityService {

    private final WebClient airQualityWebClient;
    private final String apiKey;
    private final FirebaseRealtimeService firebaseRealtimeService;

    private static final long CACHE_EXPIRATION_SECONDS = 3600;

    public AirQualityService(@Qualifier("airQualityWebClient") WebClient airQualityWebClient,
                             @Value("${api.openweathermap.api-key}") String apiKey,
                             FirebaseRealtimeService firebaseRealtimeService) {
        this.airQualityWebClient = airQualityWebClient;
        this.apiKey = apiKey;
        this.firebaseRealtimeService = firebaseRealtimeService;
    }

    public Mono<AirQuality> getAirQuality(double latitude, double longitude) {
        String cacheKey = String.format("air_quality_%.4f_%.4f", latitude, longitude)
                .replace(".", "_").replace("-", "minus");

        System.out.println(">>> [AirQualityService] Intentando obtener calidad del aire para: " + latitude + ", " + longitude);

        return firebaseRealtimeService.getGeoCache(cacheKey, AirQuality.class)
                .flatMap(cachedAirQuality -> {
                    if (cachedAirQuality != null && cachedAirQuality.getTimestamp() != null) {
                        Instant cachedTime = Instant.ofEpochSecond(cachedAirQuality.getTimestamp());
                        Instant now = Instant.now();
                        if (ChronoUnit.SECONDS.between(cachedTime, now) < CACHE_EXPIRATION_SECONDS) {
                            System.out.println("<<< [AirQualityService] Sirviendo calidad del aire desde caché para " + latitude + ", " + longitude + ". Datos: " + cachedAirQuality);
                            return Mono.just(cachedAirQuality);
                        } else {
                            System.out.println("--- [AirQualityService] Caché de calidad del aire expirado para " + latitude + ", " + longitude + ". Recargando...");
                            return Mono.empty();
                        }
                    } else {
                        System.out.println("--- [AirQualityService] flatMap: Cache miss o datos de calidad del aire inválidos. Pasando a switchIfEmpty.");
                        return Mono.empty();
                    }
                })
                .switchIfEmpty(Mono.defer(() -> {
                    System.out.println(">>> [AirQualityService] switchIfEmpty: No encontrado en caché o expirado. Llamando a OpenWeatherMap Air Pollution API.");
                    return callOpenWeatherAirPollutionApi(latitude, longitude, cacheKey);
                }))
                .doOnSuccess(finalAirQuality -> {
                    System.out.println("<<< [AirQualityService] doOnSuccess: Mono de getAirQuality completado exitosamente. Resultado: " + finalAirQuality);
                })
                .doOnError(e -> {
                    System.err.println("!!! [AirQualityService] doOnError: Error detectado en el Mono de getAirQuality: " + e.getMessage());
                    e.printStackTrace();
                })
                .onErrorResume(e -> {
                    System.err.println("!!! [AirQualityService] onErrorResume: Fallo final en AirQualityService para " + latitude + ", " + longitude + ": " + e.getMessage());
                    e.printStackTrace();
                    return Mono.error(new RuntimeException("Fallo al obtener datos de calidad del aire para " + latitude + ", " + longitude, e));
                });
    }

    private Mono<AirQuality> callOpenWeatherAirPollutionApi(double latitude, double longitude, String cacheKey) {
        String url = String.format("air_pollution?lat=%.4f&lon=%.4f&appid=%s", latitude, longitude, apiKey);
        System.out.println("--- [AirQualityService] Llamando a OpenWeatherMap Air Pollution API con URL: " + url);

        return airQualityWebClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(OpenWeatherAirPollutionResponse.class)
                .flatMap(apiResponse -> {
                    System.out.println("--- [AirQualityService] OpenWeatherMap Air Pollution API raw response: " + apiResponse);

                    if (apiResponse == null || apiResponse.getList() == null || apiResponse.getList().isEmpty()) {
                        System.out.println("--- [AirQualityService] OpenWeatherMap Air Pollution API devolvió respuesta vacía o nula para " + latitude + ", " + longitude);
                        return Mono.error(new IllegalArgumentException("No se encontraron datos de calidad del aire para la ubicación."));
                    }

                    OpenWeatherAirPollutionResponse.AirQualityData data = apiResponse.getList().get(0);

                    AirQuality airQuality = AirQuality.builder()
                            .latitude(latitude)
                            .longitude(longitude)
                            .aqi(data.getMain().getAqi())
                            .components(data.getComponents())
                            .timestamp(data.getDt())
                            .aqiCategory(AirQuality.getAqiCategory(data.getMain().getAqi()))
                            .build();

                    System.out.println("<<< [AirQualityService] Parseo exitoso a AirQuality: " + airQuality);

                    return firebaseRealtimeService.saveGeoCache(cacheKey, airQuality)
                            .thenReturn(airQuality);
                })
                .doOnError(e -> {
                    System.err.println("!!! [AirQualityService] Error en la llamada a OpenWeatherMap Air Pollution API o en el parseo: " + e.getMessage());
                    e.printStackTrace();
                });
    }
}