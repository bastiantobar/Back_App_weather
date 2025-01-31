package com.back.tfm.weatherapp.service;

import com.back.tfm.weatherapp.service.WeatherScheduler;
import com.back.tfm.weatherapp.service.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.*;

class WeatherSchedulerTest {

    @Mock
    private WeatherService weatherService;

    @InjectMocks
    private WeatherScheduler weatherScheduler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testFetchAndPersistInstantWeather() {
        when(weatherService.getAndPersistInstantWeather()).thenReturn(Mono.empty());

        weatherScheduler.fetchAndPersistInstantWeather();

        verify(weatherService, times(1)).getAndPersistInstantWeather();
    }

    @Test
    void testFetchAndPersistHourlyForecast() {
        when(weatherService.getAndPersistHourlyForecast()).thenReturn(Mono.empty());

        weatherScheduler.fetchAndPersistHourlyForecast();

        verify(weatherService, times(1)).getAndPersistHourlyForecast();
    }

    @Test
    void testFetchAndPersistWindMap() {
        when(weatherService.getAndPersistWindMap()).thenReturn(Mono.empty());

        weatherScheduler.fetchAndPersistWindMap();

        verify(weatherService, times(1)).getAndPersistWindMap();
    }
}
