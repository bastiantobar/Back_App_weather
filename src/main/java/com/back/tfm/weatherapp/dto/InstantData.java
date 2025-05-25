// src/main/java/com/back/tfm/weatherapp/dto/InstantData.java
package com.back.tfm.weatherapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Contiene detalles de las condiciones meteorológicas instantáneas.")
public class InstantData {

    @Schema(description = "Detalles de las condiciones meteorológicas instantáneas.")
    private InstantDetails details;
}