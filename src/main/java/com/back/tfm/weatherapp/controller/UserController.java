package com.back.tfm.weatherapp.controller;
import com.back.tfm.weatherapp.dto.UserDto;
import com.back.tfm.weatherapp.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Usuarios", description = "Endpoints para gestionar usuarios")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Obtener todos los usuarios", description = "Devuelve una lista de todos los usuarios")
    public List<UserDto> getAllUsers(@AuthenticationPrincipal Jwt jwt) {
        if (jwt != null) {
            System.out.println("JWT subject: " + jwt.getSubject());
        } else {
            System.out.println("JWT es nulo");
        }
        System.out.println("Solicitud recibida en /api/v1/users");
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un usuario por ID", description = "Devuelve los detalles de un usuario específico por su ID")
    public UserDto getUserById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject(); // UID del usuario autenticado
        System.out.println("Usuario autenticado: " + userId);
        return userService.getUserById(id);
    }
}
