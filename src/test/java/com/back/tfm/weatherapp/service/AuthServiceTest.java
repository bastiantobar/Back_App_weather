package com.back.tfm.weatherapp.service;

import com.back.tfm.weatherapp.service.AuthService;
import com.google.firebase.auth.*;
import com.google.firebase.database.DatabaseReference;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private RestTemplate restTemplate;
    @Mock private DatabaseReference databaseReference;
    @Mock private UserRecord mockUserRecord;

    @InjectMocks private AuthService authService;

    private static MockedStatic<FirebaseAuth> firebaseAuthMockedStatic;
    private static FirebaseAuth firebaseAuth;

    @BeforeEach
    void setUp() throws FirebaseAuthException {
        // Mockear FirebaseAuth como estático
        firebaseAuthMockedStatic = mockStatic(FirebaseAuth.class);
        firebaseAuth = mock(FirebaseAuth.class);
        firebaseAuthMockedStatic.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);

        // Simular respuesta de registro de usuario
        lenient().when(firebaseAuth.createUser(any())).thenReturn(mockUserRecord);
        lenient().when(mockUserRecord.getUid()).thenReturn("test-uid");
    }

    @AfterEach
    void tearDown() {
        // Cerrar el mock estático para evitar problemas en otros tests
        firebaseAuthMockedStatic.close();
    }

    @Test
    void shouldRegisterUserSuccessfully() throws FirebaseAuthException {
        String uid = authService.registerUser("user@example.com", "password123");

        assertNotNull(uid);
        assertEquals("test-uid", uid);
        verify(firebaseAuth, times(1)).createUser(any());
    }

   
}
