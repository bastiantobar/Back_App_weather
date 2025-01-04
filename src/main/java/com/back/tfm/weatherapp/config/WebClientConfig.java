package com.back.tfm.weatherapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder
                .baseUrl("https://api.met.no/weatherapi/")
                .defaultHeader("User-Agent", "MyWeatherApp/1.0 (bastiantobar@example.com)")
                .build();
    }
}
