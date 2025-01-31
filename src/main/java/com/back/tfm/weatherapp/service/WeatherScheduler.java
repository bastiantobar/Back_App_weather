package com.back.tfm.weatherapp.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class WeatherScheduler {

    private final WeatherService weatherService;

    public WeatherScheduler(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    // Ejecutar cada 10 minutos para InstantWeather
    @Scheduled(cron = "0 */10 * * * *")
    public void fetchAndPersistInstantWeather() {
        weatherService.getAndPersistInstantWeather()
                .doOnError(error -> System.err.println("Error al persistir InstantWeather: " + error.getMessage()))
                .subscribe();
        System.out.println("Tarea programada ejecutada: Datos InstantWeather consumidos y persistidos.");
    }

    // Ejecutar una vez al día para HourlyForecast
    @Scheduled(cron = "0 0 0 * * *")
    public void fetchAndPersistHourlyForecast() {
        weatherService.getAndPersistHourlyForecast()
                .doOnError(error -> System.err.println("Error al persistir HourlyForecast: " + error.getMessage()))
                .subscribe();
        System.out.println("Tarea programada ejecutada: Datos HourlyForecast consumidos y persistidos.");
    }

    // Ejecutar una vez al día para WindMap
    @Scheduled(cron = "0 0 0 * * *")
    public void fetchAndPersistWindMap() {
        weatherService.getAndPersistWindMap()
                .doOnError(error -> System.err.println("Error al persistir WindMap: " + error.getMessage()))
                .subscribe();
        System.out.println("Tarea programada ejecutada: Datos WindMap consumidos y persistidos.");
    }
}
