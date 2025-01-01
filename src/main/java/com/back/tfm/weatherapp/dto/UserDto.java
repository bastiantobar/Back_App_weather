package com.back.tfm.weatherapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Genera getters, setters, toString, equals, y hashCode
@AllArgsConstructor // Genera un constructor con todos los argumentos
@NoArgsConstructor  // Genera un constructor sin argumentos
public class UserDto {
    private Long id;
    private String name;
    private String email;
}