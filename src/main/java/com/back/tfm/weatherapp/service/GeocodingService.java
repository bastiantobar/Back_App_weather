package com.back.tfm.weatherapp.service;

import com.back.tfm.weatherapp.dto.LocationCoordinates;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class GeocodingService {

    private final WebClient nominatimWebClient;
    private final FirebaseRealtimeService firebaseRealtimeService;

    public GeocodingService(@Qualifier("nominatimWebClient") WebClient nominatimWebClient,
                            FirebaseRealtimeService firebaseRealtimeService) {
        this.nominatimWebClient = nominatimWebClient;
        this.firebaseRealtimeService = firebaseRealtimeService;
    }

    public Mono<LocationCoordinates> getCoordinates(String city, String country) {
        String query = String.format("%s, %s", city, country);
        String cacheKey = "geocoding_" + query.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();

        System.out.println(">>> [GeocodingService] Intentando obtener coordenadas para: " + query);

        return firebaseRealtimeService.getGeoCache(cacheKey, LocationCoordinates.class)
                .flatMap(cachedCoords -> {
                    if (cachedCoords != null && cachedCoords.getLatitude() != null && cachedCoords.getLongitude() != null && cachedCoords.getLatitude() != 0.0 && cachedCoords.getLongitude() != 0.0) {
                        System.out.println("<<< [GeocodingService] Coordenadas obtenidas desde caché para: " + query);
                        return Mono.just(cachedCoords);
                    }
                    System.out.println("--- [GeocodingService] Caché fallido o datos inválidos. Realizando llamada a Nominatim API.");
                    return Mono.empty();
                })
                .switchIfEmpty(Mono.defer(() -> {
                    System.out.println(">>> [GeocodingService] switchIfEmpty: Llamando a Nominatim API para: " + query);
                    return nominatimWebClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path("search")
                                    .queryParam("q", query)
                                    .queryParam("format", "json")
                                    .queryParam("limit", 1)
                                    .build())
                            .retrieve()
                            .onStatus(HttpStatusCode::isError, response -> {
                                System.err.println("!!! [GeocodingService] Error de HTTP de Nominatim API (onStatus): " + response.statusCode());
                                if (response.statusCode() == HttpStatus.NOT_FOUND) {
                                    return Mono.error(new IllegalArgumentException("Ubicación no encontrada: " + query));
                                }
                                return Mono.error(new RuntimeException("Error de Nominatim API: " + response.statusCode()));
                            })
                            .bodyToMono(List.class)
                            .flatMap(responseList -> {
                                if (responseList == null || responseList.isEmpty()) {
                                    System.err.println("!!! [GeocodingService] Respuesta de Nominatim vacía para: " + query);
                                    return Mono.error(new IllegalArgumentException("Ubicación no encontrada: " + query));
                                }
                                try {
                                    Map<String, Object> firstResult = (Map<String, Object>) responseList.get(0);
                                    Double lat = Double.parseDouble(firstResult.get("lat").toString());
                                    Double lon = Double.parseDouble(firstResult.get("lon").toString());
                                    String displayName = (String) firstResult.get("display_name");

                                    // Intentar extraer ciudad y país del display_name o usar los originales
                                    String resolvedCity = city;
                                    String resolvedCountry = country;
                                    if (displayName != null) {
                                        // Simple extracción, puede ser más robusta si es necesario
                                        String[] parts = displayName.split(", ");
                                        if (parts.length > 0) {
                                            resolvedCity = parts[0];
                                        }
                                        if (parts.length > 1) { // Asumiendo que el último es el país
                                            resolvedCountry = parts[parts.length - 1];
                                        }
                                    }

                                    // FIX: Usar el constructor correcto de LocationCoordinates
                                    LocationCoordinates coords = new LocationCoordinates(resolvedCity, lat, lon, resolvedCountry);
                                    System.out.println("--- [GeocodingService] Coordenadas obtenidas de Nominatim API: " + coords);
                                    return firebaseRealtimeService.saveGeoCache(cacheKey, coords)
                                            .thenReturn(coords);
                                } catch (NumberFormatException e) {
                                    System.err.println("!!! [GeocodingService] Error al parsear lat/lon a Double: " + e.getMessage());
                                    return Mono.error(new RuntimeException("Error al parsear coordenadas numéricas.", e));
                                } catch (ClassCastException e) {
                                    System.err.println("!!! [GeocodingService] Error de casting al extraer datos de Nominatim: " + e.getMessage());
                                    return Mono.error(new RuntimeException("Error de formato en la respuesta de Nominatim.", e));
                                } catch (Exception parseException) {
                                    System.err.println("!!! [GeocodingService] Error inesperado al parsear respuesta de Nominatim a LocationCoordinates: " + parseException.getMessage());
                                    parseException.printStackTrace();
                                    return Mono.error(new RuntimeException("Error al parsear respuesta de Nominatim", parseException));
                                }
                            })
                            .doOnError(e -> {
                                System.err.println("!!! [GeocodingService] Error en la llamada a Nominatim API o en el parseo: " + e.getMessage());
                                e.printStackTrace();
                            });
                }));
    }
}