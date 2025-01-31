package com.back.tfm.weatherapp.service;

import static org.mockito.Mockito.*;

import com.back.tfm.weatherapp.model.InstantWeather;
import com.google.firebase.database.DatabaseReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

        // 🔥 Permitir llamadas a `child(...)`
        when(databaseReferenceMock.child("InstantWeather")).thenReturn(childReferenceMock);
        when(childReferenceMock.push()).thenReturn(keyReferenceMock);
        when(keyReferenceMock.getKey()).thenReturn("mockKey");
        when(childReferenceMock.child("mockKey")).thenReturn(keyReferenceMock);
    }

    @Test
    void testSaveInstantWeather() {
        // 🔥 Crear un objeto de prueba
        InstantWeather mockWeather = new InstantWeather();
        mockWeather.setAirTemperature(25.0);
        mockWeather.setRelativeHumidity(50.0);
        mockWeather.setAirPressureAtSeaLevel(1013.0);
        mockWeather.setWindSpeed(10.0);
        mockWeather.setCloudAreaFraction(30.0);

        // 🔥 Llamar a la función
        firebaseRealtimeService.saveInstantWeather(mockWeather);

        // 🔥 Verificar que `child("InstantWeather")` se llamó **2 veces**
        verify(databaseReferenceMock, times(2)).child("InstantWeather");

        // 🔥 Verificar que `push()` se llamó para generar la clave
        verify(childReferenceMock).push();

        // 🔥 Verificar que se obtuvo la clave y se guardó en Firebase
        verify(keyReferenceMock).getKey();
        verify(childReferenceMock).child("mockKey");
        verify(keyReferenceMock).setValueAsync(mockWeather);
    }
}
