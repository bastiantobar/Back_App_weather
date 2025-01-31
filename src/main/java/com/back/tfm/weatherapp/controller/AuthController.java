package com.back.tfm.weatherapp.controller;

import com.back.tfm.weatherapp.dto.LoginRequestDto;
import com.back.tfm.weatherapp.dto.UserDto;
import com.back.tfm.weatherapp.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
@Tag(name = "Autenticación", description = "Endpoints para autenticación y registro de usuarios en Firebase")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(
            summary = "Registrar un nuevo usuario",
            description = "Registra un usuario en Firebase con email y contraseña",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "JSON con los datos del usuario",
                    required = true,
                    content = @Content(
                            schema = @Schema(example = "{ \"email\": \"user@example.com\", \"password\": \"password123\" }")
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario registrado exitosamente",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"message\": \"Usuario registrado con UID: abc123\" }"))),
            @ApiResponse(responseCode = "400", description = "Error en el registro",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"error\": \"Email y contraseña son obligatorios\" }")))
    })
    public ResponseEntity<Map<String, String>> registerUser(@RequestBody UserDto userDto) {
        try {
            if (userDto.getEmail() == null || userDto.getPassword() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email y contraseña son obligatorios"));
            }

            String userId = authService.registerUser(userDto.getEmail(), userDto.getPassword());
            return ResponseEntity.ok(Map.of("message", "Usuario registrado con UID: " + userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Error al registrar usuario: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    @Operation(
            summary = "Autenticar usuario",
            description = "Inicia sesión en Firebase con email y contraseña y devuelve un token JWT",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "JSON con el email y contraseña del usuario",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = LoginRequestDto.class)
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario autenticado exitosamente",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"token\": \"Bearer abcdef123456\" }"))),
            @ApiResponse(responseCode = "400", description = "Credenciales inválidas o error de autenticación",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"error\": \"Error al autenticar usuario\" }")))
    })
    public ResponseEntity<Map<String, String>> loginUser(@RequestBody LoginRequestDto loginRequest) {
        try {
            if (loginRequest.getEmail() == null || loginRequest.getPassword() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email y contraseña son obligatorios"));
            }

            String token = authService.authenticateUser(loginRequest.getEmail(), loginRequest.getPassword());

            return ResponseEntity.ok(Map.of("token", "Bearer " + token));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Error al autenticar usuario: " + e.getMessage()));
        }
    }

    @PostMapping("/update-fcm-token")
    @Operation(
            summary = "Actualizar el token de FCM del usuario",
            description = "Guarda o actualiza el token de FCM del usuario autenticado"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token de FCM actualizado correctamente",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"message\": \"Token de FCM actualizado con éxito\" }"))),
            @ApiResponse(responseCode = "400", description = "Error en la actualización",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"error\": \"Token de FCM es requerido\" }")))
    })
    public ResponseEntity<Map<String, String>> updateFcmToken(
            @RequestHeader("Authorization") String authToken,
            @RequestBody Map<String, String> request
    ) {
        try {
            String fcmToken = request.get("fcmToken");
            if (fcmToken == null || fcmToken.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Token de FCM es requerido"));
            }

            String userId = authService.getUserIdFromToken(authToken);
            authService.updateFcmTokenInDatabase(userId, fcmToken);

            return ResponseEntity.ok(Map.of("message", "Token de FCM actualizado con éxito"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Error al actualizar token de FCM: " + e.getMessage()));
        }
    }
}
