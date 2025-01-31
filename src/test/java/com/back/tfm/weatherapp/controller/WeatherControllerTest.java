package com.back.tfm.weatherapp.controller;

import com.back.tfm.weatherapp.model.HourlyForecast;
import com.back.tfm.weatherapp.model.InstantWeather;
import com.back.tfm.weatherapp.model.WindMap;
import com.back.tfm.weatherapp.service.FirebaseRealtimeService;
import com.back.tfm.weatherapp.service.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeatherControllerTest {

    @Mock
    private FirebaseRealtimeService firebaseRealtimeService;

    @Mock
    private WeatherService weatherService;

    @InjectMocks
    private WeatherController weatherController;

    private InstantWeather mockInstantWeather;
    private HourlyForecast mockHourlyForecast;
    private WindMap mockWindMap;

    @BeforeEach
    void setUp() {
        mockInstantWeather = new InstantWeather();
        mockInstantWeather.setAirTemperature(25.5);
        mockInstantWeather.setRelativeHumidity(80);
        mockInstantWeather.setAirPressureAtSeaLevel(1013);
        mockInstantWeather.setWindSpeed(15);
        mockInstantWeather.setCloudAreaFraction(50);

        mockHourlyForecast = new HourlyForecast();
        mockHourlyForecast.setTime("2025-01-31T10:00:00Z");
        mockHourlyForecast.setAirTemperature(18.3);
        mockHourlyForecast.setWindSpeed(10);
        mockHourlyForecast.setPrecipitationAmount(5.2); // Nueva propiedad en lugar de `humidity`

        mockWindMap = new WindMap();
        mockWindMap.setFeatures(Collections.emptyList()); // Simula que no hay datos
    }

    @Test
    void shouldReturnLastInstantWeather() {
        when(firebaseRealtimeService.getLastInstantWeather()).thenReturn(Mono.just(mockInstantWeather));

        ResponseEntity<InstantWeather> response = weatherController.getLastInstantWeather().block();

        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(mockInstantWeather.getAirTemperature(), response.getBody().getAirTemperature());
        verify(firebaseRealtimeService, times(1)).getLastInstantWeather();
    }

    @Test
    void shouldHandleErrorOnLastInstantWeather() {
        when(firebaseRealtimeService.getLastInstantWeather()).thenReturn(Mono.error(new RuntimeException("Firebase error")));

        ResponseEntity<InstantWeather> response = weatherController.getLastInstantWeather().block();

        assertNotNull(response);
        assertEquals(500, response.getStatusCodeValue());
        verify(firebaseRealtimeService, times(1)).getLastInstantWeather();
    }

    @Test
    void shouldReturnHourlyForecasts() {
        List<HourlyForecast> mockForecasts = List.of(mockHourlyForecast);
        when(firebaseRealtimeService.getHourlyForecastsLast48Hours()).thenReturn(Mono.just(mockForecasts));

        ResponseEntity<List<HourlyForecast>> response = weatherController.getHourlyForecastsLast48Hours().block();

        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertFalse(response.getBody().isEmpty());
        assertEquals(mockHourlyForecast.getAirTemperature(), response.getBody().get(0).getAirTemperature());
        verify(firebaseRealtimeService, times(1)).getHourlyForecastsLast48Hours();
    }

    @Test
    void shouldHandleErrorOnHourlyForecasts() {
        when(firebaseRealtimeService.getHourlyForecastsLast48Hours()).thenReturn(Mono.error(new RuntimeException("Firebase error")));

        ResponseEntity<List<HourlyForecast>> response = weatherController.getHourlyForecastsLast48Hours().block();

        assertNotNull(response);
        assertEquals(500, response.getStatusCodeValue());
        verify(firebaseRealtimeService, times(1)).getHourlyForecastsLast48Hours();
    }

    @Test
    void shouldReturnNoContentForLastWindMap() {
        when(firebaseRealtimeService.getLastWindMap()).thenReturn(Mono.just(mockWindMap));

        ResponseEntity<?> response = weatherController.getLastWindMap().block();

        assertNotNull(response);
        assertEquals(204, response.getStatusCodeValue()); // 204 significa No Content
        verify(firebaseRealtimeService, times(1)).getLastWindMap();
    }

    @Test
    void shouldHandleErrorOnLastWindMap() {
        when(firebaseRealtimeService.getLastWindMap()).thenReturn(Mono.error(new RuntimeException("Firebase error")));

        ResponseEntity<?> response = weatherController.getLastWindMap().block();

        assertNotNull(response);
        assertEquals(500, response.getStatusCodeValue());
        verify(firebaseRealtimeService, times(1)).getLastWindMap();
    }

    @Test
    void shouldReturnMeteogramAsBytes() {
        byte[] mockImage = "mock_svg_image".getBytes();
        when(weatherService.getMeteogramAsBytes()).thenReturn(Mono.just(mockImage));

        ResponseEntity<byte[]> response = weatherController.getMeteogramAsBytes().block();

        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertArrayEquals(mockImage, response.getBody());
        verify(weatherService, times(1)).getMeteogramAsBytes();
    }

    @Test
    void shouldHandleErrorOnMeteogram() {
        when(weatherService.getMeteogramAsBytes()).thenReturn(Mono.error(new RuntimeException("Error retrieving meteogram")));

        ResponseEntity<byte[]> response = weatherController.getMeteogramAsBytes().block();

        assertNotNull(response);
        assertEquals(500, response.getStatusCodeValue());
        verify(weatherService, times(1)).getMeteogramAsBytes();
    }
}
