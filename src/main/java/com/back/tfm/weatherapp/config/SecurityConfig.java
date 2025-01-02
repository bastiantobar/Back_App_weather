package com.back.tfm.weatherapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        // Configuración del URI del JWK Set para Firebase
        return NimbusReactiveJwtDecoder.withJwkSetUri("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com").build();
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf().disable()
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/auth/register").permitAll() // Permitir acceso público a /auth/register
                        .pathMatchers("/auth/validate").permitAll() // Permitir acceso público al endpoint de validación
                        .pathMatchers("/api/v1/users/**").authenticated() // Proteger endpoints de usuarios
                        .anyExchange().permitAll() // Otros endpoints son públicos
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt()) // Configuración del servidor de recursos OAuth2
                .build();
    }
}
