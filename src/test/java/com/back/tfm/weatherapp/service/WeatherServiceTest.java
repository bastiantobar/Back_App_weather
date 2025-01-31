package com.back.tfm.weatherapp.service;

import com.back.tfm.weatherapp.service.WeatherService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeatherServiceTest {

    private MockWebServer mockWebServer;
    private WeatherService weatherService;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // Crear WebClient apuntando al servidor de prueba
        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString()) // Apunta al servidor de MockWebServer
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
}
