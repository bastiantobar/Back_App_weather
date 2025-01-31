package com.back.tfm.weatherapp.service;

import com.back.tfm.weatherapp.model.HourlyForecast;
import com.back.tfm.weatherapp.model.InstantWeather;
import com.back.tfm.weatherapp.model.WindMap;
import com.google.firebase.database.DatabaseReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FirebaseRealtimeServiceTest {

    @Mock
    private DatabaseReference databaseReferenceMock; // 🔥 Mock de DatabaseReference

    @Mock
    private DatabaseReference childReferenceMock; // 🔥 Para simular `child(...)`

    @InjectMocks
    private FirebaseRealtimeService firebaseRealtimeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // 🔥 Simular que `databaseReference.child(...)` devuelve otro DatabaseReference
        when(databaseReferenceMock.child(anyString())).thenReturn(childReferenceMock);
        when(childReferenceMock.push()).thenReturn(childReferenceMock);
    }
}
