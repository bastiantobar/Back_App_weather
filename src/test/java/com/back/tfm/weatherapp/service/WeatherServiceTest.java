package com.back.tfm.weatherapp.service;

import com.back.tfm.weatherapp.model.InstantWeather;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class WeatherServiceTest {

    private MockWebServer mockWebServer;
    private WeatherService weatherService;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // Crear WebClient apuntando al servidor de prueba
        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString()) // Apunta al servidor Mock
                .build();

        weatherService = new WeatherService(webClient, null); // `null` porque no probamos Firebase aún
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void testGetLocationForecast() throws Exception {
        String mockResponse = "{\"weather\":\"sunny\"}";
        mockWebServer.enqueue(new MockResponse().setBody(mockResponse).setResponseCode(200));

        Mono<String> result = weatherService.getLocationForecast(40.4168, -3.7038);

        assertEquals(mockResponse, result.block());

        assertEquals("/locationforecast/2.0/compact?lat=40.4168&lon=-3.7038",
                mockWebServer.takeRequest().getPath());
    }

    @Test
    void testGetInstantWeather_Success() throws Exception {
        // 🔥 JSON simulado con datos reales de la API
        String mockResponse = """
        {
          "properties": {
            "timeseries": [{
              "data": {
                "instant": {
                  "details": {
                    "air_temperature": 15.5,
                    "relative_humidity": 60.0,
                    "air_pressure_at_sea_level": 1015.0,
                    "wind_speed": 5.0,
                    "cloud_area_fraction": 20.0
                  }
                }
              }
            }]
          }
        }
        """;

        // 🔥 Simula la respuesta del servidor con código 200
        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponse)
                .setHeader("Content-Type", "application/json")
                .setResponseCode(200));

        // 🔥 Llama al método real
        Mono<InstantWeather> resultMono = weatherService.getInstantWeather();
        InstantWeather result = resultMono.block(); // Bloquea para obtener el resultado

        // 🔥 Verifica que los valores fueron extraídos correctamente
        assertNotNull(result);
        assertEquals(15.5, result.getAirTemperature());
        assertEquals(60.0, result.getRelativeHumidity());
        assertEquals(1015.0, result.getAirPressureAtSeaLevel());
        assertEquals(5.0, result.getWindSpeed());
        assertEquals(20.0, result.getCloudAreaFraction());
    }
}
