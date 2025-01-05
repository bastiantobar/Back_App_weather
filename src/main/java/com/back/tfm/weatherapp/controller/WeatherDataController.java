package com.back.tfm.weatherapp.controller;

import com.back.tfm.weatherapp.model.HourlyForecast;
import com.back.tfm.weatherapp.model.InstantWeather;
import com.back.tfm.weatherapp.model.WindMap;
import com.back.tfm.weatherapp.service.FirebaseRealtimeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/weather/data")
public class WeatherDataController {

    private final FirebaseRealtimeService firebaseRealtimeService;

    public WeatherDataController(FirebaseRealtimeService firebaseRealtimeService) {
        this.firebaseRealtimeService = firebaseRealtimeService;
    }

    @GetMapping("/instant")
    public Mono<ResponseEntity<List<InstantWeather>>> getAllInstantWeather() {
        return firebaseRealtimeService.getAllInstantWeather()
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .build()));
    }

    @GetMapping("/hourly")
    public Mono<ResponseEntity<List<HourlyForecast>>> getAllHourlyForecasts() {
        return firebaseRealtimeService.getAllHourlyForecasts()
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .build()));
    }

    @GetMapping("/wind-maps")
    public Mono<ResponseEntity<List<WindMap>>> getAllWindMaps() {
        return firebaseRealtimeService.getAllWindMaps()
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .build()));
    }
}

