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
    private final TranslationService translationService; // Inyectar el nuevo servicio de traducción

    @Value("${nasa.apod.api.key}")
    private String nasaApodApiKey;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Constructor actualizado para inyectar TranslationService
    public ApodService(@Qualifier("nasaApodWebClient") WebClient nasaApodWebClient,
                       TranslationService translationService) {
        this.nasaApodWebClient = nasaApodWebClient;
        this.translationService = translationService;
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

                    // --- Lógica de Traducción para la explicación ---
                    String originalExplanation = response.getExplanation();
                    String translatedExplanation = originalExplanation; // Por defecto, el original

                    if (originalExplanation != null && !originalExplanation.trim().isEmpty()) {
                        // Asumimos que la explicación original de la NASA está en inglés ("en")
                        translatedExplanation = translationService.translateText(originalExplanation, "en", "es");
                    }
                    // ------------------------------------------------

                    return NASAApodInfo.builder()
                            .title(response.getTitle())
                            .explanation(translatedExplanation) // Usar la explicación traducida
                            .url(response.getUrl())
                            .thumbnailUrl(Optional.ofNullable(response.getThumbnailUrl()).orElse(null))
                            .copyright(Optional.ofNullable(response.getCopyright()).orElse("NASA"))
                            .mediaType(response.getMediaType())
                            .date(response.getDate())
                            .build();
                })
                .doOnError(e -> System.err.println("!!! [ApodService] Error en la llamada a NASA APOD API para la fecha " + formattedDate + ": " + e.getMessage()))
                .onErrorResume(e -> Mono.just(new NASAApodInfo()));
    }
}
