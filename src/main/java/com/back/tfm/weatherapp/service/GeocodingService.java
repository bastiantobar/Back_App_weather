package com.back.tfm.weatherapp.service;

import com.back.tfm.weatherapp.dto.LocationCoordinates;
import com.back.tfm.weatherapp.service.FirebaseRealtimeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class GeocodingService {

    private final WebClient webClient;
    private final FirebaseRealtimeService firebaseRealtimeService;

    public GeocodingService(WebClient.Builder webClientBuilder,
                            @Value("${api.nominatim.base-url}") String nominatimBaseUrl,
                            FirebaseRealtimeService firebaseRealtimeService) {
        this.webClient = webClientBuilder.baseUrl(nominatimBaseUrl).build();
        this.firebaseRealtimeService = firebaseRealtimeService;
    }

    public Mono<LocationCoordinates> getCoordinates(String city, String country) {
        String query = String.format("%s, %s", city, country);
        String cacheKey = "geocoding_" + query.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();

        System.out.println(">>> [GeocodingService] Intentando obtener coordenadas para: " + query);

        return firebaseRealtimeService.getGeoCache(cacheKey, LocationCoordinates.class)
                .doOnNext(cachedCoords -> {
                    System.out.println("--- [GeocodingService] doOnNext: Valor recibido de getGeoCache antes del flatMap: " + (cachedCoords == null ? "NULL" : cachedCoords.toString()));
                })
                .flatMap(cachedCoords -> {
                    // Si cachedCoords no es null, significa que se encontró en caché.
                    if (cachedCoords != null) {
                        System.out.println("<<< [GeocodingService] flatMap: Cache hit para " + query + ". Sirviendo desde caché. Datos: " + cachedCoords); // <-- ¡NUEVO LOG AQUÍ!
                        return Mono.just(cachedCoords);
                    } else {
                        // Si cachedCoords es null, significa que hubo un cache miss.
                        // Retornamos Mono.empty() para que switchIfEmpty lo capture.
                        System.out.println("--- [GeocodingService] flatMap: Cache miss. Retornando Mono.empty() para disparar switchIfEmpty.");
                        return Mono.empty();
                    }
                })
                .switchIfEmpty(Mono.defer(() -> {
                    System.out.println(">>> [GeocodingService] switchIfEmpty: No encontrado en caché para " + query + ". Llamando a Nominatim API.");
                    return callNominatimApi(query, cacheKey);
                }))
                .doOnSuccess(finalCoords -> {
                    System.out.println("<<< [GeocodingService] doOnSuccess: Mono de getCoordinates completado exitosamente. Resultado: " + finalCoords);
                })
                .doOnError(e -> {
                    System.err.println("!!! [GeocodingService] doOnError: Error detectado en el Mono de getCoordinates: " + e.getMessage());
                    e.printStackTrace();
                })
                .onErrorResume(e -> {
                    System.err.println("!!! [GeocodingService] onErrorResume: Fallo final en GeocodingService para " + query + ": " + e.getMessage());
                    e.printStackTrace();
                    return Mono.error(new RuntimeException("Fallo al obtener coordenadas para " + query, e));
                });
    }

    // El resto de la clase (callNominatimApi) debe mantenerse igual
    // No incluyo callNominatimApi aquí para mantener el enfoque en la función corregida,
    // pero asegúrate de que tu implementación de esa función sea la misma que la anterior.
    private Mono<LocationCoordinates> callNominatimApi(String query, String cacheKey) {
        String url = String.format("/search?q=%s&format=json&limit=1&addressdetails=1", query);
        return webClient.get()
                .uri(url)
                .header("User-Agent", "TFM-WeatherApp/1.0 (bastiantobar.94@gmail.com)") // ¡IMPORTANTE: Asegúrate de que tu email sea correcto aquí!
                .retrieve()
                .bodyToMono(List.class)
                .flatMap(responseList -> {
                    System.out.println("--- [GeocodingService] Nominatim raw response (List): " + responseList);

                    if (responseList == null || responseList.isEmpty()) {
                        System.out.println("--- [GeocodingService] Nominatim devolvió respuesta vacía o nula para " + query);
                        return Mono.error(new IllegalArgumentException("No se encontraron coordenadas para la ubicación: " + query));
                    }

                    if (!(responseList.get(0) instanceof Map)) {
                        System.err.println("!!! [GeocodingService] El primer elemento de la respuesta de Nominatim no es un Map: " + responseList.get(0).getClass().getName());
                        return Mono.error(new RuntimeException("Formato de respuesta inesperado de Nominatim."));
                    }

                    Map<String, Object> result = (Map<String, Object>) responseList.get(0);
                    System.out.println("--- [GeocodingService] Nominatim first result map: " + result);

                    try {
                        String latStr = (String) result.get("lat");
                        String lonStr = (String) result.get("lon");
                        String displayName = (String) result.get("display_name");
                        Map<String, String> address = (Map<String, String>) result.get("address");
                        String countryCode = (address != null) ? address.get("country_code") : null;

                        if (latStr == null || lonStr == null) {
                            System.err.println("!!! [GeocodingService] lat o lon son nulos en la respuesta de Nominatim: " + result);
                            return Mono.error(new RuntimeException("Latitud o Longitud no encontradas en la respuesta de Nominatim."));
                        }

                        Double lat = Double.parseDouble(latStr);
                        Double lon = Double.parseDouble(lonStr);

                        LocationCoordinates coords = LocationCoordinates.builder()
                                .name(displayName)
                                .latitude(lat)
                                .longitude(lon)
                                .countryCode(countryCode != null ? countryCode.toUpperCase() : null)
                                .build();

                        System.out.println("<<< [GeocodingService] Parseo exitoso a LocationCoordinates: " + coords);

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
    }
}