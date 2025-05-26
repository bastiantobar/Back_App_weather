// src/main/java/com/back/tfm/weatherapp/service/GeocodingService.java
package com.back.tfm.weatherapp.service;

import com.back.tfm.weatherapp.dto.LocationCoordinates; // Asegúrate de que este paquete sea correcto
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference; // ¡Importante para el mapeo de listas!
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class GeocodingService {

    private final WebClient nominatimWebClient;

    public GeocodingService(@Qualifier("nominatimWebClient") WebClient nominatimWebClient) {
        this.nominatimWebClient = nominatimWebClient;
    }

    // Método actualizado para aceptar una única cadena de búsqueda (ej. "Hijuelas, Valparaiso, Chile")
    public Mono<LocationCoordinates> getCoordinates(String addressQuery) {
        // --- INICIO DE CAMBIO PARA NORMALIZAR LA CADENA ---
        // 1. Reemplaza todas las comas por un espacio.
        // 2. Reemplaza cualquier secuencia de uno o más espacios en blanco por un solo espacio.
        // 3. Elimina espacios al inicio y al final.
        String normalizedQuery = addressQuery.replace(",", " ").replaceAll("\\s+", " ").trim();
        // --- FIN DE CAMBIO PARA NORMALIZAR LA CADENA ---

        String encodedQuery = URLEncoder.encode(normalizedQuery, StandardCharsets.UTF_8);

        System.out.println(">>> [GeocodingService] Intentando obtener coordenadas para: '" + addressQuery + "'. Cadena normalizada y codificada para Nominatim: '" + encodedQuery + "'. Realizando llamada a Nominatim.");

        // Realizar la llamada a la API de Nominatim directamente
        return callNominatimApi(encodedQuery, addressQuery) // Se pasa addressQuery original para logs/fallback
                .doOnError(e -> System.err.println("!!! [GeocodingService] Error al procesar geocodificación para '" + addressQuery + "': " + e.getMessage()))
                .onErrorResume(e -> Mono.error(new RuntimeException("Fallo al obtener coordenadas de Nominatim: " + e.getMessage())));
    }

    // Método privado para la llamada real a la API de Nominatim
    private Mono<LocationCoordinates> callNominatimApi(String encodedQuery, String originalAddressQuery) {
        String uri = String.format("/search?q=%s&format=json&limit=1", encodedQuery);

        return nominatimWebClient.get()
                .uri(uri)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> {
                    System.err.println("!!! [GeocodingService] Error al obtener coordenadas de Nominatim (código de estado): " + response.statusCode());
                    return Mono.error(new RuntimeException("Error al obtener coordenadas de Nominatim: " + response.statusCode()));
                })
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .flatMap(nominatimResponses -> {
                    if (nominatimResponses != null && !nominatimResponses.isEmpty()) {
                        Map<String, Object> firstResult = nominatimResponses.get(0);
                        try {
                            double lat = Double.parseDouble(firstResult.get("lat").toString());
                            double lon = Double.parseDouble(firstResult.get("lon").toString());

                            // Extraer display_name
                            String displayName = (String) firstResult.get("display_name");
                            // Extraer address y country_code
                            Map<String, Object> addressMap = (Map<String, Object>) firstResult.get("address");
                            String countryCode = "UNKNOWN"; // Valor por defecto
                            if (addressMap != null && addressMap.containsKey("country_code")) {
                                countryCode = ((String) addressMap.get("country_code")).toUpperCase();
                            }

                            // Usar displayName de Nominatim, o fallback a la cadena original si es nulo/vacío
                            String name = Optional.ofNullable(displayName)
                                    .filter(s -> !s.isEmpty())
                                    .orElse(originalAddressQuery);

                            System.out.println("<<< [GeocodingService] Coordenadas obtenidas de Nominatim API: Lat " + lat + ", Lon " + lon + ", Nombre: " + name + ", País: " + countryCode);
                            return Mono.just(new LocationCoordinates(name, lat, lon, countryCode));
                        } catch (NumberFormatException e) {
                            System.err.println("!!! [GeocodingService] Error al parsear lat/lon de Nominatim a Double: " + e.getMessage());
                            return Mono.error(new RuntimeException("Error al parsear coordenadas numéricas de Nominatim.", e));
                        } catch (Exception e) {
                            System.err.println("!!! [GeocodingService] Error al procesar la respuesta de Nominatim: " + e.getMessage());
                            return Mono.error(new RuntimeException("Error al procesar la respuesta de Nominatim.", e));
                        }
                    } else {
                        System.err.println("!!! [GeocodingService] No se encontraron coordenadas para la búsqueda: " + originalAddressQuery);
                        return Mono.error(new IllegalArgumentException("No se encontraron coordenadas para la ubicación proporcionada."));
                    }
                })
                .doOnError(e -> System.err.println("!!! [GeocodingService] Error en la llamada a Nominatim API o en el parseo: " + e.getMessage()))
                .onErrorResume(e -> Mono.error(new RuntimeException("Fallo al obtener coordenadas de Nominatim: " + e.getMessage())));
    }
}