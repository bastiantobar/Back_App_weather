package com.back.tfm.weatherapp.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.FirebaseDatabase;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FirebaseConfigTest {

    private FirebaseConfig firebaseConfig;
    private static MockedStatic<FirebaseApp> firebaseAppMockedStatic;
    private static MockedStatic<FirebaseDatabase> firebaseDatabaseMockedStatic;
    private static FirebaseApp mockFirebaseApp;
    private static FirebaseDatabase mockFirebaseDatabase;

    @BeforeAll
    static void setupStaticMocks() {
        firebaseAppMockedStatic = mockStatic(FirebaseApp.class);
        firebaseDatabaseMockedStatic = mockStatic(FirebaseDatabase.class);
        mockFirebaseApp = mock(FirebaseApp.class);
        mockFirebaseDatabase = mock(FirebaseDatabase.class);
    }

    @AfterAll
    static void closeStaticMocks() {
        if (firebaseAppMockedStatic != null) {
            firebaseAppMockedStatic.close();
        }
        if (firebaseDatabaseMockedStatic != null) {
            firebaseDatabaseMockedStatic.close();
        }
    }

    @BeforeEach
    void setUp() {
        firebaseConfig = new FirebaseConfig();

        firebaseAppMockedStatic.when(FirebaseApp::getApps).thenReturn(Collections.emptyList());
        firebaseAppMockedStatic.when(() -> FirebaseApp.initializeApp(any(FirebaseOptions.class)))
                .thenReturn(mockFirebaseApp);

        firebaseDatabaseMockedStatic.when(() -> FirebaseDatabase.getInstance(any(FirebaseApp.class)))
                .thenReturn(mockFirebaseDatabase);
    }

    @Test
    void testFirebaseApp_Initialization() throws Exception {
        String mockCredentials = "{ \"type\": \"service_account\", \"project_id\": \"mock-project\" }";
        InputStream mockStream = new ByteArrayInputStream(mockCredentials.getBytes(StandardCharsets.UTF_8));

        MockedStatic<GoogleCredentials> googleCredentialsMock = mockStatic(GoogleCredentials.class);
        googleCredentialsMock.when(() -> GoogleCredentials.fromStream(any(InputStream.class)))
                .thenReturn(mock(GoogleCredentials.class));

        FirebaseApp result = firebaseConfig.firebaseApp();

        assertNotNull(result);
        firebaseAppMockedStatic.verify(() -> FirebaseApp.initializeApp(any(FirebaseOptions.class)), times(1));

        googleCredentialsMock.close(); // 
    }
}
