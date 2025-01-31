package com.back.tfm.weatherapp.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        firebaseAuthMockedStatic = mockStatic(FirebaseAuth.class);
        firebaseAuthMock = mock(FirebaseAuth.class);
        mockUserRecord = mock(UserRecord.class);

        firebaseAuthMockedStatic.when(FirebaseAuth::getInstance).thenReturn(firebaseAuthMock);
        when(firebaseAuthMock.createUser(any())).thenReturn(mockUserRecord);
        when(mockUserRecord.getUid()).thenReturn("mocked-uid");

        authService = new AuthService();
    }

    @Test
    void testRegisterUser_Success() throws FirebaseAuthException {
        String email = "test@example.com";
        String password = "password123";

        String result = authService.registerUser(email, password);

        assertEquals("mocked-uid", result);
        verify(firebaseAuthMock, times(1)).createUser(any(UserRecord.CreateRequest.class));
    }
}
