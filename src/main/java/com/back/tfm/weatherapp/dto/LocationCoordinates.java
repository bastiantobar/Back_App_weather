package com.back.tfm.weatherapp.dto;

import lombok.AllArgsConstructor; // Importar si aún no está
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor; // ¡Esta es la que necesitamos añadir!

@Data
@Builder
@NoArgsConstructor // Añade esta línea
@AllArgsConstructor // Asegúrate de que esta línea esté presente si quieres mantener el constructor con todos los argumentos para el builder
public class LocationCoordinates {
    private String name;
    private Double latitude;
    private Double longitude;
    private String countryCode;
}