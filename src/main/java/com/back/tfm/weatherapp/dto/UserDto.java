package com.back.tfm.weatherapp.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Setter
@Getter
public class UserDto {
    private String email;
    private String password;

    // Constructor vacío necesario para deserialización
    public UserDto() {
    }

    public UserDto(String email, String password) {
        this.email = email;
        this.password = password;

    }

}
