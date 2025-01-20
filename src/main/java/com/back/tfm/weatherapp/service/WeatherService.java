package com.back.tfm.weatherapp.service;

import com.back.tfm.weatherapp.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpStatusCode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class WeatherService {

    private final WebClient webClient;

    private final FirebaseRealtimeService firebaseRealtimeService;

    public WeatherService(WebClient webClient, FirebaseRealtimeService firebaseRealtimeService) {
        this.webClient = webClient;
        this.firebaseRealtimeService = firebaseRealtimeService;
    }

    public Mono<String> getLocationForecast(double lat, double lon) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("locationforecast/2.0/compact")
                        .queryParam("lat", lat)
                        .queryParam("lon", lon)
                        .build())
                .retrieve()
                .bodyToMono(String.class);
    }
    public Mono<byte[]> getMeteogramAsBytes() {
        return webClient.get()
                .uri("https://www.yr.no/en/content/2-3117735/meteogram.svg?mode=dark")
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> {
                    return Mono.error(new RuntimeException("Error al obtener el meteograma: " + response.statusCode()));
                })
                .bodyToMono(byte[].class); // Devuelve el SVG como bytes
    }

    public Mono<InstantWeather> getInstantWeather() {
        return webClient.get()
                .uri("/locationforecast/2.0/compact?lat=40.4168&lon=-3.7038")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(jsonNode -> {
                    JsonNode details = jsonNode.at("/properties/timeseries/0/data/instant/details");
                    InstantWeather weather = new InstantWeather();
                    weather.setAirTemperature(details.get("air_temperature").asDouble());
                    weather.setRelativeHumidity(details.get("relative_humidity").asDouble());
                    weather.setAirPressureAtSeaLevel(details.get("air_pressure_at_sea_level").asDouble());
                    weather.setWindSpeed(details.get("wind_speed").asDouble());
                    weather.setCloudAreaFraction(details.get("cloud_area_fraction").asDouble());
                    return weather;
                });
    }
    public Mono<List<HourlyForecast>> getHourlyForecast() {
        return webClient.get()
                .uri("/locationforecast/2.0/compact?lat=40.4168&lon=-3.7038")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(jsonNode -> {
                    JsonNode timeseriesNode = jsonNode.at("/properties/timeseries");
                    List<HourlyForecast> forecasts = new ArrayList<>();
                    if (timeseriesNode.isArray()) {
                        for (JsonNode entry : timeseriesNode) {
                            HourlyForecast forecast = new HourlyForecast();
                            forecast.setTime(entry.get("time").asText());
                            forecast.setAirTemperature(entry.at("/data/instant/details/air_temperature").asDouble());
                            forecast.setWindSpeed(entry.at("/data/instant/details/wind_speed").asDouble());
                            forecast.setPrecipitationAmount(
                                    entry.at("/data/next_1_hours/details/precipitation_amount").asDouble(0.0)
                            );
                            forecasts.add(forecast);
                        }
                    }
                    return forecasts;
                });
    }

    public Mono<WindMap> getWindSpeedMap() {
        return webClient.get()
                .uri("/locationforecast/2.0/compact?lat=40.4168&lon=-3.7038")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(jsonNode -> {
                    // Extraer las series temporales de datos
                    JsonNode timeseriesNode = jsonNode.at("/properties/timeseries");

                    // Coordenadas del punto
                    List<Double> coordinates = List.of(-3.7038, 40.4168);

                    // Crear la lista de puntos (features)
                    List<WindMapPoint> features = new ArrayList<>();
                    if (timeseriesNode.isArray()) {
                        for (JsonNode entry : timeseriesNode) {
                            // Crear las propiedades de cada punto
                            Properties properties = new Properties();
                            properties.setWindSpeed(entry.at("/data/instant/details/wind_speed").asDouble());
                            properties.setWindDirection(entry.at("/data/instant/details/wind_from_direction").asDouble());
                            properties.setTime(entry.at("/time").asText());

                            // Crear la geometría de cada punto
                            Geometry geometry = new Geometry();
                            geometry.setCoordinates(coordinates);

                            // Crear el punto completo
                            WindMapPoint point = new WindMapPoint();
                            point.setType("Feature");
                            point.setGeometry(geometry);
                            point.setProperties(properties);
                            features.add(point);
                        }
                    }

                    // Crear el objeto WindMap
                    WindMap windMap = new WindMap();
                    windMap.setType("FeatureCollection");
                    windMap.setFeatures(features);
                    return windMap;
                });
    }



    public Mono<InstantWeather> getAndPersistInstantWeather() {
        return getInstantWeather()
                .doOnNext(firebaseRealtimeService::saveInstantWeather); // Persiste en Firebase
    }

    public Mono<List<HourlyForecast>> getAndPersistHourlyForecast() {
        return getHourlyForecast()
                .doOnNext(firebaseRealtimeService::saveHourlyForecasts); // Persiste en Firebase
    }

    public Mono<WindMap> getAndPersistWindMap() {
        return getWindSpeedMap()
                .doOnNext(firebaseRealtimeService::saveWindMap); // Persiste en Firebase
    }


}
