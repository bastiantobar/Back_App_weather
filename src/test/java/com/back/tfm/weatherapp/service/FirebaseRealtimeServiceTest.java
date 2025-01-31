package com.back.tfm.weatherapp.service;

import static org.mockito.Mockito.*;

import com.back.tfm.weatherapp.model.HourlyForecast;
import com.back.tfm.weatherapp.model.InstantWeather;
import com.back.tfm.weatherapp.model.WindMap;
import com.google.firebase.database.DatabaseReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

class FirebaseRealtimeServiceTest {

    private DatabaseReference databaseReferenceMock;
    private DatabaseReference childReferenceMock;
    private DatabaseReference keyReferenceMock;
    private FirebaseRealtimeService firebaseRealtimeService;

    @BeforeEach
    void setUp() {
        databaseReferenceMock = mock(DatabaseReference.class);
        childReferenceMock = mock(DatabaseReference.class);
        keyReferenceMock = mock(DatabaseReference.class);

        firebaseRealtimeService = new FirebaseRealtimeService(databaseReferenceMock);

        when(databaseReferenceMock.child("InstantWeather")).thenReturn(childReferenceMock);
        when(databaseReferenceMock.child("HourlyForecasts")).thenReturn(childReferenceMock);
        when(databaseReferenceMock.child("WindMaps")).thenReturn(childReferenceMock);
        when(childReferenceMock.push()).thenReturn(keyReferenceMock);
        when(keyReferenceMock.getKey()).thenReturn("mockKey");
        when(childReferenceMock.child("mockKey")).thenReturn(keyReferenceMock);
    }

    @Test
    void testSaveInstantWeather() {
        InstantWeather mockWeather = new InstantWeather();
        mockWeather.setAirTemperature(25.0);
        mockWeather.setRelativeHumidity(50.0);
        mockWeather.setAirPressureAtSeaLevel(1013.0);
        mockWeather.setWindSpeed(10.0);
        mockWeather.setCloudAreaFraction(30.0);

        firebaseRealtimeService.saveInstantWeather(mockWeather);

        verify(databaseReferenceMock, times(2)).child("InstantWeather");
        verify(childReferenceMock).push();
        verify(keyReferenceMock).getKey();
        verify(childReferenceMock).child("mockKey");
        verify(keyReferenceMock).setValueAsync(mockWeather);
    }

    @Test
    void testSaveHourlyForecasts() {
        HourlyForecast forecast1 = new HourlyForecast();
        forecast1.setTime("2025-02-01T12:00:00Z");
        forecast1.setAirTemperature(15.5);
        forecast1.setWindSpeed(10.0);
        forecast1.setPrecipitationAmount(0.2);

        HourlyForecast forecast2 = new HourlyForecast();
        forecast2.setTime("2025-02-01T13:00:00Z");
        forecast2.setAirTemperature(16.0);
        forecast2.setWindSpeed(12.0);
        forecast2.setPrecipitationAmount(0.3);

        List<HourlyForecast> forecasts = List.of(forecast1, forecast2);

        firebaseRealtimeService.saveHourlyForecasts(forecasts);

        verify(databaseReferenceMock, times(1)).child("HourlyForecasts");
        verify(childReferenceMock, times(2)).push();
        verify(keyReferenceMock, times(2)).getKey();
        verify(childReferenceMock, times(2)).child("mockKey");
        verify(keyReferenceMock, times(2)).setValueAsync(any(HourlyForecast.class));
    }

    @Test
    void testSaveWindMap() {
        WindMap mockWindMap = new WindMap();
        mockWindMap.setType("FeatureCollection");

        firebaseRealtimeService.saveWindMap(mockWindMap);

        verify(databaseReferenceMock, times(2)).child("WindMaps");
        verify(childReferenceMock).push();
        verify(keyReferenceMock).getKey();
        verify(childReferenceMock).child("mockKey");
        verify(keyReferenceMock).setValueAsync(mockWindMap);
    }
}
