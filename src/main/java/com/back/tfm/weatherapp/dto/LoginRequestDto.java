package com.back.tfm.weatherapp.dto;

import java.util.Objects;

public class LoginRequestDto {
    private String email;
    private String password;

    // Constructor por defecto
    public LoginRequestDto() {
    }

    // Constructor con parámetros
    public LoginRequestDto(String email, String password) {
        this.email = email;
        this.password = password;
    }

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

    // Método toString
    @Override
    public String toString() {
        return "LoginRequestDto{" +
                "email='" + email + '\'' +
                ", password='" + password + '\'' +
                '}';
    }

    // Método equals
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoginRequestDto that = (LoginRequestDto) o;
        return Objects.equals(email, that.email) &&
                Objects.equals(password, that.password);
    }

    // Método hashCode
    @Override
    public int hashCode() {
        return Objects.hash(email, password);
    }
}
