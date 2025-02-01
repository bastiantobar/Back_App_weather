package com.back.tfm.weatherapp.config;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FirebaseConfigTest {

    @InjectMocks
    private FirebaseConfig firebaseConfig;

    @Mock
    private FirebaseApp firebaseApp;

    @Mock
    private FirebaseDatabase firebaseDatabase;

    @Mock
    private DatabaseReference databaseReference;

    @Mock
    private FirebaseMessaging firebaseMessaging;

    private static MockedStatic<FirebaseApp> firebaseAppMockedStatic;

    @BeforeEach
    void setUp() {
        firebaseAppMockedStatic = mockStatic(FirebaseApp.class);
    }

}
