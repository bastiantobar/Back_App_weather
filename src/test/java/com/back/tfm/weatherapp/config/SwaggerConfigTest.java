package com.back.tfm.weatherapp.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SwaggerConfigTest {

    @InjectMocks
    private SwaggerConfig swaggerConfig;

    private OpenAPI openAPI;

    @BeforeEach
    void setUp() {
        swaggerConfig = new SwaggerConfig();
        openAPI = swaggerConfig.customOpenAPI();
    }

    @Test
    void testOpenAPIConfiguration() {
        assertNotNull(openAPI);

        assertTrue(openAPI.getSecurity().stream()
                .anyMatch(security -> security.addList("bearerAuth") != null));

        Components components = openAPI.getComponents();
        assertNotNull(components);
        assertNotNull(components.getSecuritySchemes());
        assertTrue(components.getSecuritySchemes().containsKey("bearerAuth"));

        SecurityScheme securityScheme = components.getSecuritySchemes().get("bearerAuth");
        assertNotNull(securityScheme);
        assertEquals(SecurityScheme.Type.HTTP, securityScheme.getType());
        assertEquals("bearer", securityScheme.getScheme());
        assertEquals("JWT", securityScheme.getBearerFormat());
    }

    @Test
    void testOpenAPIInfo() {
        Info info = openAPI.getInfo();
        assertNotNull(info);
        assertEquals("Weather App API", info.getTitle());
        assertEquals("1.0", info.getVersion());
        assertEquals("API para autenticación y datos meteorológicos", info.getDescription());
    }
}
