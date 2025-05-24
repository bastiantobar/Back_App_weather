package com.back.tfm.weatherapp.service;

import com.back.tfm.weatherapp.dto.AirQuality;
import com.back.tfm.weatherapp.dto.LocationCoordinates;
import com.back.tfm.weatherapp.dto.LocationForecastResponse;
import com.back.tfm.weatherapp.dto.WeatherResponse;
import com.back.tfm.weatherapp.dto.SunriseSunsetResponse;
import com.back.tfm.weatherapp.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Locale;


@Service
public class WeatherService {

    private final WebClient metNoWebClient;
    private final WebClient yrNoWebClient;
    private final AirQualityService airQualityService;
    private final FirebaseRealtimeService firebaseRealtimeService;
    private final SunriseSunsetService sunriseSunsetService;

    private static final long CACHE_EXPIRATION_WEATHER_SECONDS = 600;
    private static final long CACHE_EXPIRATION_WINDMAP_SECONDS = 1800;

    public WeatherService(@Qualifier("metNoWebClient") WebClient metNoWebClient,
                          @Qualifier("yrNoWebClient") WebClient yrNoWebClient,
                          AirQualityService airQualityService,
                          FirebaseRealtimeService firebaseRealtimeService,
                          SunriseSunsetService sunriseSunsetService) {
        this.metNoWebClient = metNoWebClient.mutate()
                .filter(logResponse())
                .build();
        this.yrNoWebClient = yrNoWebClient;
        this.airQualityService = airQualityService;
        this.firebaseRealtimeService = firebaseRealtimeService;
        this.sunriseSunsetService = sunriseSunsetService;
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            // Loggea el código de estado HTTP
            System.out.println("--- [Met.no API Filter] Status Code: " + clientResponse.statusCode());

            if (clientResponse.statusCode().isError()) {
                // Si es un error, loggea el cuerpo del error
                return clientResponse.bodyToMono(String.class)
                        .flatMap(errorBody -> {
                            System.err.println("!!! [Met.no API Filter] Error Response Body: " + errorBody);
                            return Mono.just(clientResponse.mutate().body(errorBody).build());
                        });
            } else {
                // Si es exitoso, loggea el cuerpo completo de la respuesta (truncado)
                return clientResponse.bodyToMono(String.class)
                        .flatMap(responseBody -> {
                            System.out.println("--- [Met.no API Filter] Full Response Body (first 500 chars): " + responseBody.substring(0, Math.min(responseBody.length(), 500)) + "...");
                            return Mono.just(clientResponse.mutate().body(responseBody).build());
                        });
            }
        });
    }

    public Mono<WeatherResponse> getAllWeatherData(double lat, double lon, String city, String country) {
        LocalDate today = LocalDate.now();

        return Mono.zip(
                        getLocationForecastWithCache(lat, lon),
                        airQualityService.getAirQuality(lat, lon),
                        sunriseSunsetService.getSunriseSunsetTimes(lat, lon, today)
                )
                .map(tuple -> {
                    LocationForecastResponse forecastResponse = tuple.getT1();
                    AirQuality airQuality = tuple.getT2();
                    SunriseSunsetResponse.Results astronomicalTimes = tuple.getT3();

                    InstantWeather instantWeather = getInstantWeatherFromMetNoResponse(forecastResponse);
                    List<HourlyForecast> hourlyForecasts = getHourlyForecastFromMetNoResponse(forecastResponse);
                    LocationCoordinates locationCoordinates = new LocationCoordinates(city, lat, lon, country);
                    WindMap windMap = getWindSpeedMapFromMetNoResponse(forecastResponse, city, country);

                    // Persistir en Firebase (opcional, si aún se requiere para otros fines)
                    firebaseRealtimeService.saveInstantWeather(instantWeather);
                    firebaseRealtimeService.saveHourlyForecasts(hourlyForecasts);
                    // firebaseRealtimeService.saveWindMap(windMap); // Ya no es necesario guardar el WindMap aquí si solo se usa el último punto para la respuesta

                    return WeatherResponse.builder()
                            .location(locationCoordinates)
                            .currentWeather(instantWeather)
                            .hourlyForecasts(hourlyForecasts)
                            .windMap(windMap)
                            .airQuality(airQuality)
                            .astronomicalTimes(astronomicalTimes)
                            .build();
                });
    }

    // =========================================================================
    // Métodos de parseo y lógica de negocio
    // =========================================================================

    private Mono<LocationForecastResponse> getLocationForecastWithCache(double lat, double lon) {
        String cacheKey = String.format(Locale.US, "metno_forecast_%.4f_%.4f", lat, lon)
                .replace(".", "_").replace("-", "minus");

        System.out.println(">>> [WeatherService] Intentando obtener Met.no response desde caché para key: " + cacheKey);

        return firebaseRealtimeService.getGeoCache(cacheKey, LocationForecastResponse.class)
                .flatMap(cachedResponse -> {
                    if (cachedResponse != null && cachedResponse.getProperties() != null) {
                        JsonNode meta = cachedResponse.getProperties().get("meta"); // Acceso correcto
                        if (meta != null && meta.get("updated_at") != null) { // Acceso correcto
                            try {
                                Instant cachedTime = Instant.parse(meta.get("updated_at").asText()); // Acceso correcto
                                Instant now = Instant.now();
                                if (ChronoUnit.SECONDS.between(cachedTime, now) < CACHE_EXPIRATION_WEATHER_SECONDS) {
                                    System.out.println("<<< [WeatherService] Sirviendo Met.no response desde caché para " + lat + ", " + lon + ".");
                                    return Mono.just(cachedResponse);
                                } else {
                                    System.out.println("--- [WeatherService] Caché de Met.no response expirado para " + lat + ", " + lon + ". Recargando...");
                                    return Mono.empty();
                                }
                            } catch (DateTimeParseException e) {
                                System.err.println("!!! [WeatherService] Error parseando timestamp de caché de Met.no: " + e.getMessage());
                                return Mono.empty();
                            }
                        }
                    }
                    System.out.println("--- [WeatherService] Cache miss o datos de Met.no response inválidos. Pasando a switchIfEmpty.");
                    return Mono.empty();
                })
                .switchIfEmpty(Mono.defer(() -> {
                    System.out.println(">>> [WeatherService] switchIfEmpty: No encontrado en caché o expirado. Llamando a Met.no Locationforecast API.");
                    return callMetNoLocationForecastApi(lat, lon, cacheKey);
                }))
                .doOnError(e -> System.err.println("!!! [WeatherService] Error en getLocationForecastWithCache: " + e.getMessage()));
    }

    private Mono<LocationForecastResponse> callMetNoLocationForecastApi(double lat, double lon, String cacheKey) {
        String url = String.format(Locale.US, "locationforecast/2.0/compact?lat=%.7f&lon=%.7f", lat, lon);
        System.out.println("--- [WeatherService] Llamando a Met.no Locationforecast API con path: " + url);
        return metNoWebClient.get()
                .uri(url)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> {
                    System.err.println("!!! [WeatherService] Error de HTTP de Met.no API (onStatus): " + response.statusCode());
                    return Mono.error(new RuntimeException("Error de Met.no Locationforecast API: " + response.statusCode()));
                })
                .bodyToMono(LocationForecastResponse.class)
                .flatMap(apiResponse -> {
                    System.out.println("--- [WeatherService] Met.no Locationforecast API raw response recibida y deserializada a LocationForecastResponse.");
                    return firebaseRealtimeService.saveGeoCache(cacheKey, apiResponse)
                            .thenReturn(apiResponse);
                })
                .doOnError(e -> {
                    System.err.println("!!! [WeatherService] Error en la llamada a Met.no Locationforecast API o en el parseo: " + e.getMessage());
                    e.printStackTrace();
                });
    }

    private InstantWeather getInstantWeatherFromMetNoResponse(LocationForecastResponse response) {
        if (response == null || response.getProperties() == null || !response.getProperties().has("timeseries")) {
            System.err.println("!!! [WeatherService] (InstantWeather) Respuesta de Met.no inválida o incompleta. Retornando vacío.");
            return new InstantWeather();
        }
        JsonNode timeseries = response.getProperties().get("timeseries"); // Acceso correcto
        if (timeseries == null || !timeseries.isArray() || timeseries.isEmpty()) {
            System.err.println("!!! [WeatherService] (InstantWeather) Timeseries no es un array o está vacío. Retornando vacío.");
            return new InstantWeather();
        }

        JsonNode firstEntry = timeseries.get(0); // Acceso correcto
        JsonNode data = firstEntry.get("data"); // Acceso correcto
        JsonNode instant = data.get("instant"); // Acceso correcto
        JsonNode details = instant.get("details"); // Acceso correcto

        if (details == null || details.isMissingNode()) {
            System.err.println("!!! [WeatherService] (InstantWeather) No se encontraron detalles instantáneos en la ruta. Retornando vacío.");
            return new InstantWeather();
        }

        double airTemperature = Optional.ofNullable(details.get("air_temperature")).map(JsonNode::asDouble).orElse(0.0);
        double relativeHumidity = Optional.ofNullable(details.get("relative_humidity")).map(JsonNode::asDouble).orElse(0.0);
        double airPressureAtSeaLevel = Optional.ofNullable(details.get("air_pressure_at_sea_level")).map(JsonNode::asDouble).orElse(0.0);
        double windSpeed = Optional.ofNullable(details.get("wind_speed")).map(JsonNode::asDouble).orElse(0.0);
        double cloudAreaFraction = Optional.ofNullable(details.get("cloud_area_fraction")).map(JsonNode::asDouble).orElse(0.0);

        return new InstantWeather(airTemperature, relativeHumidity, airPressureAtSeaLevel, windSpeed, cloudAreaFraction);
    }

    private List<HourlyForecast> getHourlyForecastFromMetNoResponse(LocationForecastResponse response) {
        List<HourlyForecast> hourlyForecasts = new ArrayList<>();
        if (response == null || response.getProperties() == null || !response.getProperties().has("timeseries")) {
            System.err.println("!!! [WeatherService] (HourlyForecast) Respuesta de Met.no inválida o incompleta. Retornando lista vacía.");
            return Collections.emptyList();
        }
        JsonNode timeseries = response.getProperties().get("timeseries"); // Acceso correcto

        if (timeseries != null && timeseries.isArray()) {
            int limit = Math.min(timeseries.size(), 24);
            for (int i = 0; i < limit; i++) {
                JsonNode hourData = timeseries.get(i); // Acceso correcto
                String time = hourData.get("time").asText(); // Acceso correcto
                JsonNode data = hourData.get("data"); // Acceso correcto
                JsonNode instantDetails = data.get("instant").get("details"); // Acceso correcto

                JsonNode next1HoursDetails = data.has("next_1_hours") ? data.get("next_1_hours").get("details") : null;
                JsonNode next6HoursDetails = data.has("next_6_hours") ? data.get("next_6_hours").get("details") : null;


                double airTemperature = Optional.ofNullable(instantDetails.get("air_temperature")).map(JsonNode::asDouble).orElse(0.0);
                double windSpeed = Optional.ofNullable(instantDetails.get("wind_speed")).map(JsonNode::asDouble).orElse(0.0);
                double precipitationAmount = 0.0;

                if (next1HoursDetails != null && next1HoursDetails.has("precipitation_amount")) {
                    precipitationAmount = next1HoursDetails.get("precipitation_amount").asDouble(0.0);
                } else if (next6HoursDetails != null && next6HoursDetails.has("precipitation_amount")) {
                    precipitationAmount = next6HoursDetails.get("precipitation_amount").asDouble(0.0) / 6.0; // Simplificación
                }

                hourlyForecasts.add(new HourlyForecast(time, airTemperature, windSpeed, precipitationAmount));
            }
        }
        return hourlyForecasts;
    }

    public WindMap getWindSpeedMapFromMetNoResponse(LocationForecastResponse response, String city, String country) {
        List<WindMapPoint> features = new ArrayList<>();

        if (response == null || response.getProperties() == null || !response.getProperties().has("timeseries")) {
            System.err.println("!!! [WeatherService] (WindMap) Respuesta de Met.no inválida o incompleta. Retornando vacío.");
            return new WindMap();
        }
        JsonNode timeseries = response.getProperties().get("timeseries"); // Acceso correcto

        if (timeseries != null && timeseries.isArray()) {
            JsonNode geometryNode = response.getGeometry(); // Acceso correcto
            double latitude = 0.0;
            double longitude = 0.0;
            if (geometryNode != null && geometryNode.has("coordinates") && geometryNode.get("coordinates").isArray()) {
                longitude = geometryNode.get("coordinates").get(0).asDouble(); // Acceso correcto
                latitude = geometryNode.get("coordinates").get(1).asDouble(); // Acceso correcto
            }

            // Iterar sobre todos los elementos para extraer posibles puntos de viento
            for (JsonNode timesery : timeseries) { // Acceso correcto
                try {
                    String time = timesery.get("time").asText(); // Acceso correcto
                    JsonNode data = timesery.get("data"); // Acceso correcto
                    JsonNode instantDetails = data.get("instant").get("details"); // Acceso correcto

                    double windSpeed = Optional.ofNullable(instantDetails.get("wind_speed")).map(JsonNode::asDouble).orElse(0.0);
                    double windFromDirection = Optional.ofNullable(instantDetails.get("wind_from_direction")).map(JsonNode::asDouble).orElse(0.0);

                    Geometry geometry = new Geometry("Point", List.of(longitude, latitude));
                    Properties properties = new Properties(windSpeed, windFromDirection, time);
                    features.add(new WindMapPoint(geometry, properties, "Feature"));

                } catch (DateTimeParseException e) {
                    System.err.println("!!! [WeatherService] Error parseando fecha en WindMap: " + timesery.get("time").asText() + " - " + e.getMessage());
                } catch (Exception e) {
                    System.err.println("!!! [WeatherService] Error procesando timesery para WindMap: " + e.getMessage());
                }
            }
        } else {
            System.err.println("!!! [WeatherService] (WindMap) /timeseries no es un array o está nulo.");
        }

        // Encuentra el WindMapPoint con el 'time' más reciente (mayor Instant)
        Optional<WindMapPoint> latestFeatureOptional = features.stream()
                .max(Comparator.comparing(f -> Instant.parse(f.getProperties().getTime())));

        if (latestFeatureOptional.isPresent()) {
            WindMapPoint latestFeature = latestFeatureOptional.get();
            System.out.println("--- [WeatherService] Seleccionado el último WindMapPoint con hora: " + latestFeature.getProperties().getTime());
            WindMap singlePointWindMap = new WindMap();
            singlePointWindMap.setType("FeatureCollection");
            singlePointWindMap.setFeatures(List.of(latestFeature));
            return singlePointWindMap;
        } else {
            System.out.println("--- [WeatherService] No se encontraron WindMapPoints para generar el mapa de viento. Retornando mapa vacío.");
            return new WindMap();
        }
    }


    // =========================================================================
    // MÉTODOS EXISTENTES QUE SE PUEDEN MANTENER SI AÚN SE USAN DIRECTAMENTE
    // =========================================================================

    public Mono<byte[]> getMeteogramAsBytes() {
        String path = "en/content/2-3117735/meteogram.svg?mode=dark";
        System.out.println("--- [WeatherService] Llamando a Yr.no para meteograma con path: " + path);

        return yrNoWebClient.get()
                .uri(path)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> {
                    System.err.println("!!! [WeatherService] Error al obtener el meteograma de Yr.no: " + response.statusCode());
                    return Mono.error(new RuntimeException("Error al obtener el meteograma: " + response.statusCode()));
                })
                .bodyToMono(byte[].class)
                .doOnError(e -> System.err.println("!!! [WeatherService] Error en la llamada a Yr.no para meteograma: " + e.getMessage()));
    }
}