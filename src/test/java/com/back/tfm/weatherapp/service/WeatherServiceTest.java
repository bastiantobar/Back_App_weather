package com.back.tfm.weatherapp.service;

import com.back.tfm.weatherapp.model.HourlyForecast;
import com.back.tfm.weatherapp.model.InstantWeather;
import com.back.tfm.weatherapp.model.WindMap;
import com.back.tfm.weatherapp.model.WindMapPoint;
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
import static org.mockito.Mockito.*;
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
    @Test
    void testGetWindSpeedMap_Success() throws Exception {
        // 🔥 JSON simulado con múltiples registros de viento
        String mockResponse = """
    {
      "properties": {
        "timeseries": [
          {
            "time": "2025-01-31T12:00:00Z",
            "data": {
              "instant": {
                "details": {
                  "wind_speed": 5.2,
                  "wind_from_direction": 180.0
                }
              }
            }
          },
          {
            "time": "2025-01-31T13:00:00Z",
            "data": {
              "instant": {
                "details": {
                  "wind_speed": 6.8,
                  "wind_from_direction": 200.0
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
        Mono<WindMap> resultMono = weatherService.getWindSpeedMap();
        WindMap result = resultMono.block(); // Bloquea para obtener el resultado

        // 🔥 Verifica que el objeto WindMap no sea nulo y tenga el tipo correcto
        assertNotNull(result);
        assertEquals("FeatureCollection", result.getType());

        // 🔥 Verifica que contiene dos puntos de datos
        List<WindMapPoint> features = result.getFeatures();
        assertNotNull(features);
        assertEquals(2, features.size());

        // 🔥 Verifica los datos del primer punto
        WindMapPoint firstPoint = features.get(0);
        assertEquals("Feature", firstPoint.getType());
        assertEquals(5.2, firstPoint.getProperties().getWindSpeed());
        assertEquals(180.0, firstPoint.getProperties().getWindDirection());
        assertEquals("2025-01-31T12:00:00Z", firstPoint.getProperties().getTime());

        // 🔥 Verifica las coordenadas del primer punto
        List<Double> expectedCoordinates = List.of(-3.7038, 40.4168);
        assertEquals(expectedCoordinates, firstPoint.getGeometry().getCoordinates());

        // 🔥 Verifica los datos del segundo punto
        WindMapPoint secondPoint = features.get(1);
        assertEquals("Feature", secondPoint.getType());
        assertEquals(6.8, secondPoint.getProperties().getWindSpeed());
        assertEquals(200.0, secondPoint.getProperties().getWindDirection());
        assertEquals("2025-01-31T13:00:00Z", secondPoint.getProperties().getTime());

        // 🔥 Verifica las coordenadas del segundo punto
        assertEquals(expectedCoordinates, secondPoint.getGeometry().getCoordinates());
    }


    @Test
    void testGetAndPersistInstantWeather_Success() throws Exception {
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

        // 🔥 Mockea FirebaseRealtimeService
        FirebaseRealtimeService firebaseRealtimeServiceMock = mock(FirebaseRealtimeService.class);
        weatherService = new WeatherService(WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build(), firebaseRealtimeServiceMock); // Inyectamos el mock

        // 🔥 Llama al método real
        Mono<InstantWeather> resultMono = weatherService.getAndPersistInstantWeather();
        InstantWeather result = resultMono.block(); // Bloquea para obtener el resultado

        // 🔥 Verifica que `getInstantWeather()` devolvió el objeto correcto
        assertNotNull(result);
        assertEquals(15.5, result.getAirTemperature());
        assertEquals(60.0, result.getRelativeHumidity());
        assertEquals(1015.0, result.getAirPressureAtSeaLevel());
        assertEquals(5.0, result.getWindSpeed());
        assertEquals(20.0, result.getCloudAreaFraction());

        // 🔥 Verifica que el servicio de Firebase se llamó con el objeto correcto
        verify(firebaseRealtimeServiceMock, times(1)).saveInstantWeather(result);
    }


}
