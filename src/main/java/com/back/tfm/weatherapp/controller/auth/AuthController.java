package com.back.tfm.weatherapp.controller.auth;

import com.back.tfm.weatherapp.service.FirebaseAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final FirebaseAuthService authService;

    public AuthController(FirebaseAuthService authService) {
        this.authService = authService;
    }

    // Registro de usuario
    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody Map<String, String> user) {
        try {
            String uid = authService.registerUser(user.get("email"), user.get("password"));
            return ResponseEntity.ok("Usuario registrado con UID: " + uid);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al registrar usuario: " + e.getMessage());
        }
    }

    // Validar token (simulación de login)
    @PostMapping("/validate")
    public ResponseEntity<String> validateToken(@RequestHeader("Authorization") String token) {
        try {
            String uid = authService.validateToken(token.replace("Bearer ", ""));
            return ResponseEntity.ok("Token válido para UID: " + uid);
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Token inválido: " + e.getMessage());
        }
    }
}
