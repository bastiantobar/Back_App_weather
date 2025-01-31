package com.back.tfm.weatherapp.controller;

import com.back.tfm.weatherapp.dto.LoginRequestDto;
import com.back.tfm.weatherapp.dto.UserDto;
import com.back.tfm.weatherapp.service.AuthService;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.FirebaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private UserDto testUserDto;
    private LoginRequestDto testLoginRequest;

    @BeforeEach
    void setUp() {
        testUserDto = new UserDto();
        testUserDto.setEmail("user@example.com");
        testUserDto.setPassword("password123");

        testLoginRequest = new LoginRequestDto();
        testLoginRequest.setEmail("user@example.com");
        testLoginRequest.setPassword("password123");
    }

    @Test
    void testRegisterUser_Success() throws FirebaseAuthException {
        when(authService.registerUser(anyString(), anyString())).thenReturn("uid-123");

        ResponseEntity<Map<String, String>> response = authController.registerUser(testUserDto);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Usuario registrado con UID: uid-123", response.getBody().get("message"));
    }



    @Test
    void testLoginUser_Success() throws Exception {
        when(authService.authenticateUser(anyString(), anyString())).thenReturn("jwt-token-123");

        ResponseEntity<Map<String, String>> response = authController.loginUser(testLoginRequest);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Bearer jwt-token-123", response.getBody().get("token"));
    }

    @Test
    void testLoginUser_InvalidCredentials() {
        try {
            when(authService.authenticateUser(anyString(), anyString()))
                    .thenThrow(new Exception("Invalid credentials"));

            Exception exception = assertThrows(Exception.class, () -> {
                authService.authenticateUser("wrong@example.com", "badpassword");
            });

            assertEquals("Invalid credentials", exception.getMessage());

        } catch (Exception e) {
            fail("Se lanzó una excepción inesperada: " + e.getMessage());
        }
    }

    @Test
    void testUpdateFcmToken_Success() throws Exception {
        String authToken = "Bearer jwt-token-123";
        Map<String, String> request = Map.of("fcmToken", "test-fcm-token");

        when(authService.getUserIdFromToken(anyString())).thenReturn("user-uid");
        doNothing().when(authService).updateFcmTokenInDatabase(anyString(), anyString());

        ResponseEntity<Map<String, String>> response = authController.updateFcmToken(authToken, request);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Token de FCM actualizado con éxito", response.getBody().get("message"));
    }

    @Test
    void testUpdateFcmToken_MissingToken() {
        Map<String, String> request = Map.of();

        ResponseEntity<Map<String, String>> response = authController.updateFcmToken("Bearer jwt-token-123", request);

        assertEquals(400, response.getStatusCodeValue());
        assertEquals("Token de FCM es requerido", response.getBody().get("error"));
    }

    @Test
    void testUpdateFcmToken_ExceptionThrown() {
        try {
            String authToken = "Bearer jwt-token-123";
            Map<String, String> request = Map.of("fcmToken", "test-fcm-token");

            when(authService.getUserIdFromToken(anyString()))
                    .thenThrow(new Exception("Error al obtener UID"));

            Exception exception = assertThrows(Exception.class, () -> {
                authService.getUserIdFromToken(authToken);
            });

            assertEquals("Error al obtener UID", exception.getMessage());

        } catch (Exception e) {
            fail("Se lanzó una excepción inesperada: " + e.getMessage());
        }
    }
}
