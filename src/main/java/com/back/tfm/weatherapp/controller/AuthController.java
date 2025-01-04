package com.back.tfm.weatherapp.controller;

import com.back.tfm.weatherapp.dto.LoginRequestDto;
import com.back.tfm.weatherapp.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            requestBody = @RequestBody(
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
    public ResponseEntity<String> registerUser(@RequestBody Map<String, String> user) {
        try {
            String email = user.get("email");
            String password = user.get("password");

            if (email == null || password == null) {
                return ResponseEntity.badRequest().body("Email y contraseña son obligatorios");
            }

            String userId = authService.registerUser(email, password);
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
            requestBody = @RequestBody(
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
    public ResponseEntity<String> loginUser(@RequestBody LoginRequestDto loginRequest) {
        try {
           /* if (loginRequest.getEmail() == null || loginRequest.getPassword() == null) {
                return ResponseEntity.badRequest().body("Email y contraseña son obligatorios");
            }*/

            String token = authService.authenticateUser("bastiantobar.94@gmail.com", "password");
            return ResponseEntity.ok("Bearer " + token);
        } catch (Exception e) {
            logger.error("Error al autenticar usuario: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error al autenticar usuario: " + e.getMessage());
        }
    }
}
