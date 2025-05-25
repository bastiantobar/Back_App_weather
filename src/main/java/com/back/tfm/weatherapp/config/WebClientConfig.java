package com.back.tfm.weatherapp.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    // WebClient para la API de Met.no (Locationforecast)
    @Bean
    public WebClient metNoWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl("https://api.met.no/weatherapi/")
                .defaultHeader("User-Agent", "MyWeatherApp/1.0 (bastiantobar@example.com)")
                .build();
    }

    // WebClient para la API de Nominatim (Geocodificación)
    @Bean
    public WebClient nominatimWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl("https://nominatim.openstreetmap.org/")
                .defaultHeader("User-Agent", "MyWeatherApp/1.0 (bastiantobar@example.com)")
                .build();
    }

    // WebClient para la API de Yr.no (Meteogramas u otros recursos de su dominio)
    @Bean
    public WebClient yrNoWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl("https://www.yr.no/")
                .defaultHeader("User-Agent", "MyWeatherApp/1.0 (bastiantobar@example.com)")
                .build();
    }

    // Nuevo WebClient para la API de Calidad del Aire de OpenWeatherMap
    @Bean
    public WebClient airQualityWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl("https://api.openweathermap.org/data/2.5/") // Base URL para la API de calidad del aire
                .defaultHeader("User-Agent", "MyWeatherApp/1.0 (bastiantobar@example.com)")
                .build();
    }
    // Nuevo WebClient para la API de Sunrise-Sunset.org
    @Bean
    public WebClient sunriseSunsetWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl("https://api.sunrise-sunset.org/")
                .defaultHeader("User-Agent", "MyWeatherApp/1.0 (bastiantobar@example.com)")
                .build();
    }

    @Bean
    @Qualifier("nasaApodWebClient") // Nuevo WebClient para NASA APOD
    public WebClient nasaApodWebClient() {
        return WebClient.builder()
                .baseUrl("https://api.nasa.gov") // URL base de NASA APOD
                .build();
    }


}