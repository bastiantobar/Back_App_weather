package com.back.tfm.weatherapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        return NimbusReactiveJwtDecoder
                .withJwkSetUri("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com")
                .build();
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable) // Deshabilitar CSRF para APIs REST
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/auth/register", "/auth/login").permitAll() // Endpoints públicos
                        .pathMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/webjars/**").permitAll() // Swagger público
                        .pathMatchers("/weather/**").authenticated() // Endpoints protegidos
                        .anyExchange().denyAll() // Bloquear cualquier otro endpoint no especificado
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt()) // Habilitar validación JWT
                .build();
    }
}
