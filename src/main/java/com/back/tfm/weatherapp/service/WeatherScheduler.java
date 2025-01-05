package com.back.tfm.weatherapp.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class WeatherScheduler {

    private final WeatherService weatherService;

    public WeatherScheduler(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    // Ejecutar cada minuto
  //  @Scheduled(cron = "0 * * * * *")
    public void fetchAndPersistWeatherData() {
        // Consume y persiste datos instantáneos
        weatherService.getAndPersistInstantWeather()
                .doOnError(error -> System.err.println("Error al persistir InstantWeather: " + error.getMessage()))
                .subscribe();

        // Consume y persiste datos horarios
        weatherService.getAndPersistHourlyForecast()
                .doOnError(error -> System.err.println("Error al persistir HourlyForecast: " + error.getMessage()))
                .subscribe();

        // Consume y persiste mapas de viento
        weatherService.getAndPersistWindMap()
                .doOnError(error -> System.err.println("Error al persistir WindMap: " + error.getMessage()))
                .subscribe();

        System.out.println("Tarea programada ejecutada: Datos consumidos y persistidos.");
    }
}
