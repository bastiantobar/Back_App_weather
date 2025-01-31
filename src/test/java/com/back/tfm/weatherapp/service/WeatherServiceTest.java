package com.back.tfm.weatherapp.service;

import com.back.tfm.weatherapp.model.HourlyForecast;
import com.back.tfm.weatherapp.model.InstantWeather;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;

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
    @Test
    void testGetHourlyForecast_Success() throws Exception {
        // 🔥 JSON simulado con múltiples registros horarios
        String mockResponse = """
    {
      "properties": {
        "timeseries": [
          {
            "time": "2025-01-31T12:00:00Z",
            "data": {
              "instant": {
                "details": {
                  "air_temperature": 10.5,
                  "wind_speed": 3.2
                }
              },
              "next_1_hours": {
                "details": {
                  "precipitation_amount": 0.8
                }
              }
            }
          },
          {
            "time": "2025-01-31T13:00:00Z",
            "data": {
              "instant": {
                "details": {
                  "air_temperature": 11.0,
                  "wind_speed": 4.5
                }
              },
              "next_1_hours": {
                "details": {
                  "precipitation_amount": 0.0
                }
              }
            }
          }
        ]
      }
    }
    """;

        // 🔥 Simula la respuesta de la API con el header correcto
        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponse)
                .setHeader("Content-Type", "application/json")
                .setResponseCode(200));

        // 🔥 Llama al método real
        Mono<List<HourlyForecast>> resultMono = weatherService.getHourlyForecast();
        List<HourlyForecast> result = resultMono.block(); // Bloquea para obtener la lista

        // 🔥 Verifica que la lista no es nula y tiene el tamaño esperado
        assertNotNull(result);
        assertEquals(2, result.size()); // 🔥 Se enviaron 2 registros

        // 🔥 Verifica que los valores fueron extraídos correctamente
        HourlyForecast first = result.get(0);
        assertEquals("2025-01-31T12:00:00Z", first.getTime());
        assertEquals(10.5, first.getAirTemperature());
        assertEquals(3.2, first.getWindSpeed());
        assertEquals(0.8, first.getPrecipitationAmount());

        HourlyForecast second = result.get(1);
        assertEquals("2025-01-31T13:00:00Z", second.getTime());
        assertEquals(11.0, second.getAirTemperature());
        assertEquals(4.5, second.getWindSpeed());
        assertEquals(0.0, second.getPrecipitationAmount()); // 🔥 Se asegura que el default (0.0) funcione
    }

}
