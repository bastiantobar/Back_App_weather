package com.back.tfm.weatherapp.controller;

import com.back.tfm.weatherapp.dto.LoginRequestDto;
import com.back.tfm.weatherapp.dto.UserDto;
import com.back.tfm.weatherapp.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Endpoints for user authentication and registration")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(
            summary = "Register a new user",
            description = "Registers a new user in Firebase using an email and password",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "JSON object containing the email and password of the user to be registered",
                    required = true,
                    content = @Content(
                            schema = @Schema(example = "{ \"email\": \"user@example.com\", \"password\": \"password123\" }")
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User registered successfully", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid input or registration error", content = @Content)
    })
    public ResponseEntity<String> registerUser(@RequestBody UserDto userDto) {
        try {
            logger.warn("Datos recibidos: email={}, password={}", userDto.getEmail(), userDto.getPassword());

            if (userDto.getEmail() == null || userDto.getPassword() == null) {
                logger.warn("Email o contraseña no proporcionados");
                return ResponseEntity.badRequest().body("Email y contraseña son obligatorios");
            }

            // Llamada al servicio de autenticación para registrar al usuario
            String userId = authService.registerUser(userDto.getEmail(), userDto.getPassword());
            return ResponseEntity.ok("Usuario registrado con UID: " + userId);
        } catch (Exception e) {
            logger.error("Error al registrar usuario: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error al registrar usuario: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    @Operation(
            summary = "Authenticate user",
            description = "Authenticates a user in Firebase using email and password, and returns a JWT token",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "JSON object containing the email and password of the user to be authenticated",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = LoginRequestDto.class)
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User authenticated successfully", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid credentials or authentication error", content = @Content)
    })
    public ResponseEntity<Map<String, String>> loginUser(@RequestBody LoginRequestDto loginRequest) {
        try {
            logger.warn("Intentando autenticar usuario con email={}", loginRequest.getEmail());

            if (loginRequest.getEmail() == null || loginRequest.getPassword() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email y contraseña son obligatorios"));
            }

            // Llamada al servicio de autenticación para validar al usuario
            String token = authService.authenticateUser(loginRequest.getEmail(), loginRequest.getPassword());

            // Construir la respuesta como JSON
            Map<String, String> response = new HashMap<>();
            response.put("token", "Bearer " + token);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error al autenticar usuario: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Error al autenticar usuario: " + e.getMessage()));
        }
    }
    @PostMapping("/update-fcm-token")
    @Operation(
            summary = "Actualizar el token de FCM del usuario",
            description = "Actualiza el token de FCM asociado al UID del usuario autenticado"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token de FCM actualizado con éxito", content = @Content),
            @ApiResponse(responseCode = "400", description = "Error al actualizar el token de FCM", content = @Content)
    })
    public ResponseEntity<String> updateFcmToken(
            @RequestHeader("Authorization") String authToken,
            @RequestBody Map<String, String> request
    ) {
        try {
            String fcmToken = request.get("fcmToken");
            logger.warn("Intentando autenticar usuario con fcmToken={}", fcmToken);
            if (fcmToken == null || fcmToken.isEmpty()) {
                return ResponseEntity.badRequest().body("Token de FCM es requerido");
            }

            // Obtener el UID del usuario autenticado desde el token de Firebase
            String userId = authService.getUserIdFromToken(authToken);
            logger.warn("Intentando autenticar usuario con userId={}", userId);

            // Guardar el FCM Token en Firebase Realtime Database o Firestore
            authService.updateFcmTokenInDatabase(userId, fcmToken);

            return ResponseEntity.ok("Token de FCM actualizado con éxito");
        } catch (Exception e) {
            logger.error("Error al actualizar token de FCM: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error al actualizar token de FCM: " + e.getMessage());
        }
    }


}
