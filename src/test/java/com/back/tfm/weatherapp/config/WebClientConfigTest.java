package com.back.tfm.weatherapp.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebClientConfigTest {

    @InjectMocks
    private WebClientConfig webClientConfig;

    @Mock
    private WebClient.Builder webClientBuilderMock;

    @Mock
    private WebClient webClientMock;

    @BeforeEach
    void setUp() {
        when(webClientBuilderMock.baseUrl("https://api.met.no/weatherapi/")).thenReturn(webClientBuilderMock);
        when(webClientBuilderMock.defaultHeader("User-Agent", "MyWeatherApp/1.0 (bastiantobar@example.com)")).thenReturn(webClientBuilderMock);
        when(webClientBuilderMock.build()).thenReturn(webClientMock);
    }


}
