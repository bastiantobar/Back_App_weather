package com.back.tfm.weatherapp.service;

import com.back.tfm.weatherapp.dto.AirQuality;
import com.back.tfm.weatherapp.dto.LocationCoordinates;
import com.back.tfm.weatherapp.dto.LocationForecastResponse;
import com.back.tfm.weatherapp.dto.WeatherResponse;
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
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Locale;


@Service
public class WeatherService {

    private final WebClient metNoWebClient;
    private final WebClient yrNoWebClient;
    private final AirQualityService airQualityService;
    private final FirebaseRealtimeService firebaseRealtimeService;

    private static final long CACHE_EXPIRATION_WEATHER_SECONDS = 600;
    private static final long CACHE_EXPIRATION_WINDMAP_SECONDS = 1800;

    public WeatherService(@Qualifier("metNoWebClient") WebClient metNoWebClient,
                          @Qualifier("yrNoWebClient") WebClient yrNoWebClient,
                          AirQualityService airQualityService,
                          FirebaseRealtimeService firebaseRealtimeService) {
        this.metNoWebClient = metNoWebClient.mutate()
                .filter(logResponse())
                .build();
        this.yrNoWebClient = yrNoWebClient;
        this.airQualityService = airQualityService;
        this.firebaseRealtimeService = firebaseRealtimeService;
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (clientResponse.statusCode().isError()) {
                System.err.println("!!! [WebClientFilter] Error HTTP: " + clientResponse.statusCode());
            }
            return Mono.just(clientResponse);
        });
    }

    public Mono<WeatherResponse> getAllWeatherData(double lat, double lon, String city, String country) {
        return Mono.zip(
                        getLocationForecastWithCache(lat, lon),
                        airQualityService.getAirQuality(lat, lon)
                )
                .map(tuple -> {
                    LocationForecastResponse forecastResponse = tuple.getT1();
                    AirQuality airQuality = tuple.getT2();

                    InstantWeather instantWeather = getInstantWeatherFromMetNoResponse(forecastResponse);
                    List<HourlyForecast> hourlyForecasts = getHourlyForecastFromMetNoResponse(forecastResponse);
                    LocationCoordinates locationCoordinates = new LocationCoordinates(city, lat, lon, country);
                    WindMap windMap = getWindSpeedMapFromMetNoResponse(forecastResponse, city, country);

                    // Persistir en Firebase (opcional, si aún se requiere para otros fines)
                    firebaseRealtimeService.saveInstantWeather(instantWeather);
                    firebaseRealtimeService.saveHourlyForecasts(hourlyForecasts);
                    firebaseRealtimeService.saveWindMap(windMap);

                    return WeatherResponse.builder()
                            .location(locationCoordinates)
                            // CORRECCIÓN: Usar 'currentWeather' para coincidir con el DTO
                            .currentWeather(instantWeather) // <-- CAMBIO AQUÍ
                            .hourlyForecasts(hourlyForecasts)
                            .windMap(windMap)
                            .airQuality(airQuality)
                            .build();
                });
    }

    private Mono<LocationForecastResponse> getLocationForecastWithCache(double lat, double lon) {
        String cacheKey = String.format(Locale.US, "metno_forecast_%.4f_%.4f", lat, lon)
                .replace(".", "_").replace("-", "minus");

        System.out.println(">>> [WeatherService] Intentando obtener Met.no response desde caché para key: " + cacheKey);

        return firebaseRealtimeService.getGeoCache(cacheKey, LocationForecastResponse.class)
                .flatMap(cachedResponse -> {
                    if (cachedResponse != null && cachedResponse.getProperties() != null) {
                        JsonNode meta = cachedResponse.getProperties().get("meta");
                        if (meta != null && meta.get("updated_at") != null) {
                            try {
                                Instant cachedTime = Instant.parse(meta.get("updated_at").asText());
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
        String url = String.format(Locale.US, "locationforecast/2.0/compact?lat=%.4f&lon=%.4f", lat, lon);
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
        JsonNode data = response.getProperties().get("timeseries").get(0).get("data");
        JsonNode instant = data.get("instant").get("details");

        double airTemperature = Optional.ofNullable(instant.get("air_temperature")).map(JsonNode::asDouble).orElse(0.0);
        double relativeHumidity = Optional.ofNullable(instant.get("relative_humidity")).map(JsonNode::asDouble).orElse(0.0);
        double airPressureAtSeaLevel = Optional.ofNullable(instant.get("air_pressure_at_sea_level")).map(JsonNode::asDouble).orElse(0.0);
        double windSpeed = Optional.ofNullable(instant.get("wind_speed")).map(JsonNode::asDouble).orElse(0.0);
        double cloudAreaFraction = Optional.ofNullable(instant.get("cloud_area_fraction")).map(JsonNode::asDouble).orElse(0.0);

        return new InstantWeather(airTemperature, relativeHumidity, airPressureAtSeaLevel, windSpeed, cloudAreaFraction);
    }

    private List<HourlyForecast> getHourlyForecastFromMetNoResponse(LocationForecastResponse response) {
        List<HourlyForecast> hourlyForecasts = new ArrayList<>();
        JsonNode timeseries = response.getProperties().get("timeseries");

        if (timeseries != null && timeseries.isArray()) {
            int limit = Math.min(timeseries.size(), 24);
            for (int i = 0; i < limit; i++) {
                JsonNode hourData = timeseries.get(i);
                String time = hourData.get("time").asText();
                JsonNode details = hourData.get("data").get("next_1_hours").get("details");
                JsonNode instantDetails = hourData.get("data").get("instant").get("details");

                double airTemperature = Optional.ofNullable(instantDetails.get("air_temperature")).map(JsonNode::asDouble).orElse(0.0);
                double windSpeed = Optional.ofNullable(instantDetails.get("wind_speed")).map(JsonNode::asDouble).orElse(0.0);
                double precipitationAmount = Optional.ofNullable(details.get("precipitation_amount")).map(JsonNode::asDouble).orElse(0.0);

                hourlyForecasts.add(new HourlyForecast(time, airTemperature, windSpeed, precipitationAmount));
            }
        }
        return hourlyForecasts;
    }

    public WindMap getWindSpeedMapFromMetNoResponse(LocationForecastResponse response, String city, String country) {
        WindMap windMap = new WindMap();
        List<WindMapPoint> features = new ArrayList<>();
        JsonNode timeseries = response.getProperties().get("timeseries");

        if (timeseries != null && timeseries.isArray()) {
            JsonNode geometryNode = response.getGeometry();
            double latitude = 0.0;
            double longitude = 0.0;
            if (geometryNode != null && geometryNode.has("coordinates") && geometryNode.get("coordinates").isArray()) {
                longitude = geometryNode.get("coordinates").get(0).asDouble();
                latitude = geometryNode.get("coordinates").get(1).asDouble();
            }

            for (JsonNode hourData : timeseries) {
                String time = hourData.get("time").asText();
                JsonNode details = hourData.get("data").get("instant").get("details");

                double windSpeed = Optional.ofNullable(details.get("wind_speed")).map(JsonNode::asDouble).orElse(0.0);
                double windDirection = Optional.ofNullable(details.get("wind_from_direction")).map(JsonNode::asDouble).orElse(0.0);

                Geometry geometry = new Geometry("Point", List.of(longitude, latitude));
                Properties properties = new Properties(windSpeed, windDirection, time);

                features.add(new WindMapPoint(geometry, properties, "Feature"));
            }
            if (features.isEmpty()) {
                System.out.println("--- [WeatherService] (WindMap) Timeseries está vacío, no se puede generar el mapa de viento.");
            }
        } else {
            System.err.println("!!! [WeatherService] (WindMap) /timeseries no es un array o está ausente.");
        }
        windMap.setFeatures(features);
        return windMap;
    }

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