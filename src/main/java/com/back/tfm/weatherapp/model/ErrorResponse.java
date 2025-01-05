package com.back.tfm.weatherapp.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Modelo de error para respuestas HTTP")
public class ErrorResponse {
    @Schema(description = "Código de error", example = "403")
    private int statusCode;

    @Schema(description = "Mensaje de error", example = "Acceso denegado")
    private String message;

    @Schema(description = "Detalles adicionales sobre el error", example = "Usuario no tiene permisos")
    private String details;
}
