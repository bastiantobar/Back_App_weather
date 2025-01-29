package com.back.tfm.weatherapp.model;

public class UserPreferences {
    private String email;
    private boolean temperature;
    private boolean wind;
    private boolean humidity;
    private String userId;

    // Constructor vacío necesario para Firebase
    public UserPreferences() {
    }

    public UserPreferences(String email, boolean temperature, boolean wind, boolean humidity, String userId) {
        this.email = email;
        this.temperature = temperature;
        this.wind = wind;
        this.humidity = humidity;
        this.userId = userId;
    }

    // Getters y Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isTemperature() {
        return temperature;
    }

    public void setTemperature(boolean temperature) {
        this.temperature = temperature;
    }

    public boolean isWind() {
        return wind;
    }

    public void setWind(boolean wind) {
        this.wind = wind;
    }

    public boolean isHumidity() {
        return humidity;
    }

    public void setHumidity(boolean humidity) {
        this.humidity = humidity;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
