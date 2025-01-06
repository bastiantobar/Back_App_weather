package com.back.tfm.weatherapp.dto;

public class UserDto {
    private String email;
    private String password;

    // Constructor vacío requerido para la deserialización
    public UserDto() {}

    // Getters y Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
