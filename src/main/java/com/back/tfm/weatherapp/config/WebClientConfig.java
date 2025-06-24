package com.back.tfm.weatherapp.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${api.nominatim.base-url}")
    private String nominatimBaseUrl;

    @Value("${api.metno.base-url}")
    private String metnoBaseUrl;

    @Value("${api.metno.user-agent}")
    private String metnoUserAgent;

    @Value("${api.openweathermap.air-quality.base-url}")
    private String openWeatherMapAirQualityBaseUrl;

    @Value("${api.sunrisesunset.base-url}")
    private String sunriseSunsetBaseUrl;

    @Value("${api.nasa.apod.base-url}")
    private String nasaApodBaseUrl;

    @Value("${api.yr.no.base-url}")
    private String yrnoBaseUrl;


    @Bean
    public WebClient metNoWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl("https://api.met.no/weatherapi/")
                .defaultHeader("User-Agent", metnoUserAgent)
                .build();
    }


    @Bean
    public WebClient nominatimWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(nominatimBaseUrl)
                .defaultHeader("User-Agent", metnoUserAgent)
                .build();
    }


    @Bean
    public WebClient yrNoWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(yrnoBaseUrl)
                .defaultHeader("User-Agent", metnoUserAgent)
                .build();
    }

    // Nuevo WebClient para la API de Calidad del Aire de OpenWeatherMap
    @Bean
    public WebClient airQualityWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(openWeatherMapAirQualityBaseUrl)
                .defaultHeader("User-Agent", metnoUserAgent)
                .build();
    }
    // Nuevo WebClient para la API de Sunrise-Sunset.org
    @Bean
    public WebClient sunriseSunsetWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(sunriseSunsetBaseUrl)
                .defaultHeader("User-Agent", metnoUserAgent)
                .build();
    }

    @Bean
    @Qualifier("nasaApodWebClient")
    public WebClient nasaApodWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(nasaApodBaseUrl)
                .build();
    }


}