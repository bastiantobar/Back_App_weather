package com.back.tfm.weatherapp.controller;

import ch.qos.logback.classic.Logger;
import com.back.tfm.weatherapp.dto.LoginRequestDto;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Endpoints for user authentication and registration")
public class AuthController {
    private static final Logger logger = (Logger) LoggerFactory.getLogger(AuthController.class);

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

            // Lógica para registrar usuario en Firebase
            UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                    .setEmail(email)
                    .setPassword(password);

            UserRecord userRecord = FirebaseAuth.getInstance().createUser(request);
            return ResponseEntity.ok("Usuario registrado con UID: " + userRecord.getUid());
        } catch (FirebaseAuthException e) {
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
        Logger logger = (Logger) LoggerFactory.getLogger(AuthController.class);
        logger.warn("Objeto recibido: {}", loginRequest);
        if (loginRequest != null) {
            logger.warn("Email: {}, Password: {}", loginRequest.getEmail(), loginRequest.getPassword());
        } else {
            logger.error("El objeto loginRequest es null");
        }

        // Log del objeto recibido
        logger.warn("Datos recibidos en el cuerpo de la solicitud: {}", loginRequest);


        try {
            // Extraer email y contraseña
            String email = "bastiantobar.94@gmail.com";
            String password = "password";

            logger.warn("Email recibido: {}", email);
            logger.warn("Contraseña recibida: {}", password);

            if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
                logger.warn("Email o contraseña están vacíos");
                return ResponseEntity.badRequest().body("Email y contraseña son obligatorios");
            }

            // Autenticación con Firebase
            String firebaseToken = authenticateWithFirebase(email, password);
            logger.info("Token obtenido de Firebase: {}", firebaseToken);

            return ResponseEntity.ok("Bearer " + firebaseToken);
        } catch (Exception e) {
            logger.error("Error al autenticar usuario: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error al autenticar usuario: " + e.getMessage());
        }
    }
  private String authenticateWithFirebase(String email, String password) throws Exception {
        String firebaseApiKey = "AIzaSyBQ4F2VK9t0dza3J9YX5qvx2DXtinW8u5U";
        String firebaseAuthUrl = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + firebaseApiKey;

        RestTemplate restTemplate = new RestTemplate();

        Map<String, String> request = Map.of(
                "email", email,
                "password", password,
                "returnSecureToken", "true"
        );

        try {
            // La respuesta se mapea a un HashMap
            HashMap<String, Object> response = restTemplate.postForObject(firebaseAuthUrl, request, HashMap.class);
            return (String) response.get("idToken"); // Extrae el token del JSON
        } catch (Exception e) {
            throw new Exception("Firebase authentication failed: " + e.getMessage());
        }


    }
}
