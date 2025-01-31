package com.back.tfm.weatherapp.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static MockedStatic<FirebaseAuth> firebaseAuthMockedStatic;
    private static FirebaseAuth firebaseAuthMock;
    private static UserRecord mockUserRecord;

    @InjectMocks
    private AuthService authService;


    @BeforeEach
    void setUp() throws FirebaseAuthException {
        if (firebaseAuthMockedStatic == null) {
            firebaseAuthMockedStatic = mockStatic(FirebaseAuth.class);
        }
        firebaseAuthMock = mock(FirebaseAuth.class);
        mockUserRecord = mock(UserRecord.class);

        firebaseAuthMockedStatic.when(FirebaseAuth::getInstance).thenReturn(firebaseAuthMock);
        lenient().when(firebaseAuthMock.createUser(any())).thenReturn(mockUserRecord);
        lenient().when(mockUserRecord.getUid()).thenReturn("mocked-uid");

        authService = new AuthService();
    }

    @AfterEach
    void tearDown() {
        if (firebaseAuthMockedStatic != null) {
            firebaseAuthMockedStatic.close();
            firebaseAuthMockedStatic = null;
        }
    }

    @Test
    void testRegisterUser_Success() throws FirebaseAuthException {
        String email = "test@example.com";
        String password = "password123";

        String result = authService.registerUser(email, password);

        assertEquals("mocked-uid", result);
        verify(firebaseAuthMock, times(1)).createUser(any(UserRecord.CreateRequest.class));
    }

    @Test
    void testGetUserIdFromToken_Success() throws Exception {
        String authToken = "Bearer mocked_token";
        String expectedUid = "mocked-uid";

        FirebaseToken mockFirebaseToken = mock(FirebaseToken.class);

        when(firebaseAuthMock.verifyIdToken("mocked_token")).thenReturn(mockFirebaseToken);
        when(mockFirebaseToken.getUid()).thenReturn(expectedUid);

        String result = authService.getUserIdFromToken(authToken);

        assertNotNull(result);
        assertEquals(expectedUid, result);
        verify(firebaseAuthMock, times(1)).verifyIdToken("mocked_token");
        verify(mockFirebaseToken, times(1)).getUid();
    }
}
