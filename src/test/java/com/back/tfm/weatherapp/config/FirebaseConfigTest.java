package com.back.tfm.weatherapp.config;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Collections;

@ExtendWith(MockitoExtension.class)
class FirebaseConfigTest {

    private FirebaseConfig firebaseConfig;
    private static MockedStatic<FirebaseApp> firebaseAppMockedStatic;
    private static FirebaseApp mockFirebaseApp;

    @BeforeAll
    static void setupStaticMock() {
        firebaseAppMockedStatic = mockStatic(FirebaseApp.class);
        mockFirebaseApp = mock(FirebaseApp.class);
    }

    @AfterAll
    static void closeStaticMock() {
        if (firebaseAppMockedStatic != null) {
            firebaseAppMockedStatic.close();
        }
    }

    @BeforeEach
    void setUp() {
        firebaseConfig = new FirebaseConfig();
        firebaseAppMockedStatic.when(FirebaseApp::getApps).thenReturn(Collections.emptyList());
        firebaseAppMockedStatic.when(() -> FirebaseApp.initializeApp(any(FirebaseOptions.class)))
                .thenReturn(mockFirebaseApp);
    }

    @Test
    void testFirebaseApp_Initialization() throws Exception {
        FirebaseApp result = firebaseConfig.firebaseApp();

        assertNotNull(result);
        firebaseAppMockedStatic.verify(() -> FirebaseApp.initializeApp(any(FirebaseOptions.class)), times(1));
    }
}
