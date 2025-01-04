package com.back.tfm.weatherapp.service;

import org.apache.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpStatusCode;


@Service
public class WeatherService {

    private final WebClient webClient;

    public WeatherService(WebClient webClient) {
        this.webClient = webClient;
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

}
