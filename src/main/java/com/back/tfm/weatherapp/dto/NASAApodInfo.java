// src/main/java/com/back/tfm/weatherapp/dto/NASAApodInfo.java
package com.back.tfm.weatherapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Información de la Imagen Astronómica del Día (APOD) de la NASA")
public class NASAApodInfo {
    @Schema(description = "Título de la imagen APOD")
    private String title;
    @Schema(description = "Explicación científica o descripción de la imagen")
    private String explanation;
    @Schema(description = "URL de la imagen (o video) de alta resolución")
    private String url;
    @Schema(description = "URL de la miniatura si es un video")
    private String thumbnailUrl; // Para videos, la URL principal puede ser del video, y esta de la miniatura.
    @Schema(description = "Créditos o Copyright de la imagen/video")
    private String copyright;
    @Schema(description = "Tipo de medio (image, video)")
    private String mediaType;
    @Schema(description = "Fecha de la imagen APOD")
    private String date;
}