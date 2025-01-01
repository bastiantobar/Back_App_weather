package com.back.tfm.weatherapp.service;
import org.springframework.stereotype.Service;
import com.back.tfm.weatherapp.dto.UserDto;

import java.util.Arrays;
import java.util.List;

@Service
public class UserService {
    public List<UserDto> getAllUsers() {
        // Datos simulados (mock)
        return Arrays.asList(
                new UserDto(1L, "Alice", "alice@example.com"),
                new UserDto(2L, "Bob", "bob@example.com"),
                new UserDto(3L, "Charlie", "charlie@example.com")
        );
    }

    public UserDto getUserById(Long id) {
        // Simulación de búsqueda por ID
        return new UserDto(id, "Mock User", "mockuser@example.com");
    }
}
