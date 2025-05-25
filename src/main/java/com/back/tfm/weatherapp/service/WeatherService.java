package com.back.tfm.weatherapp.service;

import com.back.tfm.weatherapp.dto.*; // Importa todos los DTOs
import com.back.tfm.weatherapp.model.*; // Importa todos los modelos de la carpeta

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
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
    // private final NeoService neoService; // Eliminado

    private static final long CACHE_EXPIRATION_WEATHER_SECONDS = 600;

    public WeatherService(@Qualifier("metNoWebClient") WebClient metNoWebClient,
                          @Qualifier("yrNoWebClient") WebClient yrNoWebClient,
                          AirQualityService airQualityService,
                          FirebaseRealtimeService firebaseRealtimeService,
                          SunriseSunsetService sunriseSunsetService
            /* , NeoService neoService */) { // Eliminado del constructor
        this.metNoWebClient = metNoWebClient.mutate()
                .filter(logResponse())
                .build();
        this.yrNoWebClient = yrNoWebClient;
        this.airQualityService = airQualityService;
        this.firebaseRealtimeService = firebaseRealtimeService;
        this.sunriseSunsetService = sunriseSunsetService;
        // this.neoService = neoService; // Eliminado
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            System.out.println("--- [Met.no API Filter] Status Code: " + clientResponse.statusCode());
            if (clientResponse.statusCode().isError()) {
                return clientResponse.bodyToMono(String.class)
                        .flatMap(errorBody -> {
                            System.err.println("!!! [Met.no API Filter] Error Response Body: " + errorBody);
                            return Mono.just(clientResponse.mutate().body(errorBody).build());
                        });
            } else {
                return clientResponse.bodyToMono(String.class)
                        .flatMap(responseBody -> {
                            System.out.println("--- [Met.no API Filter] Full Response Body (first 500 chars): " + responseBody.substring(0, Math.min(responseBody.length(), 500)) + "...");
                            return Mono.just(clientResponse.mutate().body(responseBody).build());
                        });
            }
        });
    }

    /**
     * Obtiene todos los datos meteorológicos (pronóstico, calidad del aire, amanecer/atardecer)
     * y los consolida en un objeto WeatherResponse.
     * Utiliza caché para los datos de Met.no, calidad del aire y amanecer/atardecer.
     *
     * @param lat     Latitud de la ubicación.
     * @param lon     Longitud de la ubicación.
     * @param city    Nombre de la ciudad.
     * @param country Nombre del país.
     * @return Mono que emite un objeto WeatherResponse consolidado.
     */
    public Mono<WeatherResponse> getAllWeatherData(double lat, double lon, String city, String country) {
        LocalDate today = LocalDate.now();
        // LocalDate sevenDaysAgo = today.minusDays(7); // Eliminado

        // Monos para las llamadas a las APIs
        Mono<LocationForecastResponse> metnoForecastMono = getLocationForecastWithCache(lat, lon);
        Mono<AirQuality> airQualityMono = airQualityService.getAirQuality(lat, lon);
        Mono<SunriseSunsetResponse.Results> sunriseSunsetMono = sunriseSunsetService.getSunriseSunsetTimes(lat, lon, today);
        // Mono<List<NeoFeedResponse.NearEarthObject>> neoMono = neoService.getNearEarthObjects(sevenDaysAgo, today) // Eliminado
        //         .onErrorResume(e -> {
        //             System.err.println("!!! [WeatherService] Fallo al obtener NEOs: " + e.getMessage());
        //             return Mono.just(Collections.emptyList());
        //         });

        // Combina todos los Monos (ahora solo 3)
        return Mono.zip(
                        metnoForecastMono,
                        airQualityMono,
                        sunriseSunsetMono
                )
                .map(tuple -> {
                    LocationForecastResponse forecastResponse = tuple.getT1();
                    AirQuality airQuality = tuple.getT2();
                    SunriseSunsetResponse.Results astronomicalTimes = tuple.getT3();
                    // List<NeoFeedResponse.NearEarthObject> nearEarthObjects = tuple.getT4(); // Eliminado

                    InstantWeather instantWeather = getInstantWeatherFromMetNoResponse(forecastResponse);
                    List<HourlyForecast> hourlyForecasts = getHourlyForecastFromMetNoResponse(forecastResponse);
                    LocationCoordinates locationCoordinates = new LocationCoordinates(city, lat, lon, country);
                    WindMap windMap = getWindSpeedMapFromMetNoResponse(forecastResponse, city, country);

                    return WeatherResponse.builder()
                            .location(locationCoordinates)
                            .currentWeather(instantWeather)
                            .hourlyForecasts(hourlyForecasts)
                            .windMap(windMap)
                            .airQuality(airQuality)
                            .astronomicalTimes(astronomicalTimes)
                            // .nearEarthObjects(nearEarthObjects) // Eliminado
                            .build();
                })
                .doOnError(e -> System.err.println("!!! [WeatherService] Error consolidando datos en getAllWeatherData: " + e.getMessage()));
    }

    /**
     * Obtiene el pronóstico de Met.no, utilizando caché de Firebase.
     *
     * @param lat Latitud.
     * @param lon Longitud.
     * @return Mono que emite LocationForecastResponse.
     */
    private Mono<LocationForecastResponse> getLocationForecastWithCache(double lat, double lon) {
        String cacheKey = String.format(Locale.US, "metno_forecast_%.4f_%.4f", lat, lon)
                .replace(".", "_").replace("-", "minus");

        System.out.println(">>> [WeatherService] Intentando obtener Met.no response desde caché para key: " + cacheKey);

        return firebaseRealtimeService.getGeoCache(cacheKey, LocationForecastResponse.class)
                .flatMap(cachedResponse -> {
                    if (cachedResponse != null && cachedResponse.getProperties() != null && cachedResponse.getProperties().getMeta() != null) {
                        LocationForecastMeta meta = cachedResponse.getProperties().getMeta();
                        if (meta.getUpdated_at() != null) {
                            try {
                                Instant cachedTime = Instant.parse(meta.getUpdated_at());
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
                    if (apiResponse != null && apiResponse.getProperties() != null && apiResponse.getProperties().getTimeseries() != null && !apiResponse.getProperties().getTimeseries().isEmpty()) {
                        System.out.println("--- [WeatherService] Met.no Locationforecast API raw response recibida y deserializada a LocationForecastResponse.");
                        return firebaseRealtimeService.saveGeoCache(cacheKey, apiResponse)
                                .thenReturn(apiResponse);
                    } else {
                        System.err.println("!!! [WeatherService] Respuesta vacía o nula de Met.no API.");
                        return Mono.error(new RuntimeException("API de Met.no devolvió respuesta vacía o nula."));
                    }
                })
                .doOnError(e -> {
                    System.err.println("!!! [WeatherService] Error en la llamada a Met.no Locationforecast API o en el parseo: " + e.getMessage());
                    e.printStackTrace();
                });
    }

    /**
     * Extrae el InstantWeather del LocationForecastResponse.
     *
     * @param response La respuesta completa de Met.no.
     * @return InstantWeather.
     */
    private InstantWeather getInstantWeatherFromMetNoResponse(LocationForecastResponse response) {
        if (response == null || response.getProperties() == null || response.getProperties().getTimeseries() == null || response.getProperties().getTimeseries().isEmpty()) {
            System.err.println("!!! [WeatherService] (InstantWeather) Respuesta de Met.no inválida o incompleta. Retornando vacío.");
            return new InstantWeather();
        }

        TimeseriesData firstEntry = response.getProperties().getTimeseries().get(0);
        LocationForecastData data = firstEntry.getData();
        InstantData instant = data.getInstant();
        InstantDetails details = instant.getDetails();

        if (details == null) {
            System.err.println("!!! [WeatherService] (InstantWeather) No se encontraron detalles instantáneos en la ruta. Retornando vacío.");
            return new InstantWeather();
        }

        return InstantWeather.builder()
                .airTemperature(Optional.ofNullable(details.getAir_temperature()).orElse(0.0))
                .relativeHumidity(Optional.ofNullable(details.getRelative_humidity()).orElse(0.0))
                .airPressureAtSeaLevel(Optional.ofNullable(details.getAir_pressure_at_sea_level()).orElse(0.0))
                .windSpeed(Optional.ofNullable(details.getWind_speed()).orElse(0.0))
                .cloudAreaFraction(Optional.ofNullable(details.getCloud_area_fraction()).orElse(0.0))
                .build();
    }

    /**
     * Extrae la lista de HourlyForecasts del LocationForecastResponse.
     *
     * @param response La respuesta completa de Met.no.
     * @return Lista de HourlyForecast.
     */
    private List<HourlyForecast> getHourlyForecastFromMetNoResponse(LocationForecastResponse response) {
        List<HourlyForecast> hourlyForecasts = new ArrayList<>();
        if (response == null || response.getProperties() == null || response.getProperties().getTimeseries() == null || response.getProperties().getTimeseries().isEmpty()) {
            System.err.println("!!! [WeatherService] (HourlyForecast) Respuesta de Met.no inválida o incompleta. Retornando lista vacía.");
            return Collections.emptyList();
        }

        List<TimeseriesData> timeseries = response.getProperties().getTimeseries();
        int limit = Math.min(timeseries.size(), 24);

        for (int i = 0; i < limit; i++) {
            TimeseriesData hourData = timeseries.get(i);
            String time = hourData.getTime();
            LocationForecastData data = hourData.getData();
            InstantDetails instantDetails = data.getInstant() != null ? data.getInstant().getDetails() : null;

            NextHoursData next1Hours = data.getNext1Hours();
            NextHoursData next6Hours = data.getNext6Hours();

            double airTemperature = (instantDetails != null && instantDetails.getAir_temperature() != null) ? instantDetails.getAir_temperature() : 0.0;
            double windSpeed = (instantDetails != null && instantDetails.getWind_speed() != null) ? instantDetails.getWind_speed() : 0.0;
            double precipitationAmount = 0.0;

            if (next1Hours != null && next1Hours.getDetails() != null && next1Hours.getDetails().getPrecipitation_amount() != null) {
                precipitationAmount = next1Hours.getDetails().getPrecipitation_amount();
            } else if (next6Hours != null && next6Hours.getDetails() != null && next6Hours.getDetails().getPrecipitation_amount() != null) {
                precipitationAmount = next6Hours.getDetails().getPrecipitation_amount() / 6.0;
            }

            hourlyForecasts.add(new HourlyForecast(time, airTemperature, windSpeed, precipitationAmount));
        }
        return hourlyForecasts;
    }

    public WindMap getWindSpeedMapFromMetNoResponse(LocationForecastResponse response, String city, String country) {
        List<WindMapPoint> features = new ArrayList<>();

        if (response == null || response.getProperties() == null || response.getProperties().getTimeseries() == null || response.getProperties().getTimeseries().isEmpty()) {
            System.err.println("!!! [WeatherService] (WindMap) Respuesta de Met.no inválida o incompleta. Retornando vacío.");
            return new WindMap();
        }

        List<TimeseriesData> timeseries = response.getProperties().getTimeseries();
        LocationForecastGeometry geometryDto = response.getGeometry();

        double latitude = 0.0;
        double longitude = 0.0;
        if (geometryDto != null && geometryDto.getCoordinates() != null && geometryDto.getCoordinates().size() >= 2) {
            longitude = geometryDto.getCoordinates().get(0);
            latitude = geometryDto.getCoordinates().get(1);
        } else {
            System.err.println("!!! [WeatherService] (WindMap) Coordenadas de geometría no disponibles. Usando 0,0.");
        }

        for (TimeseriesData timesery : timeseries) {
            try {
                String time = timesery.getTime();
                LocationForecastData data = timesery.getData();
                InstantDetails instantDetails = data.getInstant() != null ? data.getInstant().getDetails() : null;

                if (instantDetails != null) {
                    double windSpeed = Optional.ofNullable(instantDetails.getWind_speed()).orElse(0.0);
                    double windFromDirection = Optional.ofNullable(instantDetails.getWind_from_direction()).orElse(0.0);

                    com.back.tfm.weatherapp.model.Geometry geometry = new com.back.tfm.weatherapp.model.Geometry("Point", List.of(longitude, latitude));
                    com.back.tfm.weatherapp.model.Properties properties = new com.back.tfm.weatherapp.model.Properties(windSpeed, windFromDirection, time);
                    features.add(new WindMapPoint(geometry, properties, "Feature"));
                }

            } catch (Exception e) {
                System.err.println("!!! [WeatherService] Error procesando timesery para WindMap: " + e.getMessage());
            }
        }

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