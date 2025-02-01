package com.back.tfm.weatherapp.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.mock.web.server.MockServerWebExchange;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @InjectMocks
    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig();
    }
    @Test
    void testCorsConfigurationSource() {
        CorsConfigurationSource corsConfigurationSource = securityConfig.corsConfigurationSource();
        assertNotNull(corsConfigurationSource);

        ServerWebExchange exchange = MockServerWebExchange.builder(
                MockServerHttpRequest.get("/test").build()
        ).build();

        var corsConfig = corsConfigurationSource.getCorsConfiguration(exchange);

        assertNotNull(corsConfig);
        assertTrue(corsConfig.getAllowedOrigins().contains("*"));
        assertTrue(corsConfig.getAllowedMethods().contains("GET"));
        assertTrue(corsConfig.getAllowedHeaders().contains("Authorization"));
        assertFalse(corsConfig.getAllowCredentials());
    }
    @Test
    void testReactiveJwtDecoder() {
        ReactiveJwtDecoder jwtDecoder = securityConfig.reactiveJwtDecoder();
        assertNotNull(jwtDecoder);
    }
    @Test
    void testSecurityWebFilterChain() {
        ServerHttpSecurity http = mock(ServerHttpSecurity.class, RETURNS_DEEP_STUBS);

        assertDoesNotThrow(() -> securityConfig.securityWebFilterChain(http));
    }



}
