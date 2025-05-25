// src/main/java/com/back/tfm/weatherapp/service/ApodService.java
package com.back.tfm.weatherapp.service;

import com.back.tfm.weatherapp.dto.NASAApodInfo;
import com.back.tfm.weatherapp.dto.NASAApodResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Optional;

@Service
public class ApodService {

    private final WebClient nasaApodWebClient;
    @Value("${nasa.apod.api.key}") // Asegúrate de añadir esta propiedad en application.properties
    private String nasaApodApiKey;

    // Formateador para la fecha de la API (YYYY-MM-DD)
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ApodService(@Qualifier("nasaApodWebClient") WebClient nasaApodWebClient) {
        this.nasaApodWebClient = nasaApodWebClient;
    }

    public Mono<NASAApodInfo> getApodInfo(LocalDate date) {
        String formattedDate = date.format(DATE_FORMATTER);
        String uri = String.format("/planetary/apod?api_key=%s&date=%s", nasaApodApiKey, formattedDate);

        return nasaApodWebClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(NASAApodResponse.class)
                .map(response -> {
                    System.out.println("--- [ApodService] NASA APOD API response recibida y deserializada para la fecha: " + formattedDate);
                    return NASAApodInfo.builder()
                            .title(response.getTitle())
                            .explanation(response.getExplanation())
                            .url(response.getUrl())
                            .thumbnailUrl(Optional.ofNullable(response.getThumbnailUrl()).orElse(null)) // Puede ser nulo para imágenes
                            .copyright(Optional.ofNullable(response.getCopyright()).orElse("NASA")) // Proporciona un valor por defecto si es nulo
                            .mediaType(response.getMediaType())
                            .date(response.getDate())
                            .build();
                })
                .doOnError(e -> System.err.println("!!! [ApodService] Error en la llamada a NASA APOD API para la fecha " + formattedDate + ": " + e.getMessage()))
                .onErrorResume(e -> Mono.just(new NASAApodInfo())); // Retorna un objeto vacío en caso de error
    }
}