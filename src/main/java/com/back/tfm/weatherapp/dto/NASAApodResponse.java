// src/main/java/com/back/tfm/weatherapp/dto/nasaapod/NASAApodResponse.java
package com.back.tfm.weatherapp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // Ignora campos que no necesitemos de la respuesta
public class NASAApodResponse {
    private String copyright;
    private String date;
    private String explanation;
    private String hdurl; // URL de alta resolución (puede ser la misma que 'url' para imágenes)
    @JsonProperty("media_type") // Mapea "media_type" del JSON a mediaType
    private String mediaType;
    @JsonProperty("service_version")
    private String serviceVersion;
    private String title;
    private String url; // URL principal de la imagen/video
    @JsonProperty("thumbnail_url") // Para videos, es la URL de la miniatura
    private String thumbnailUrl;
}