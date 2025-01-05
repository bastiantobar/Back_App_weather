package com.back.tfm.weatherapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RestAppWeatherApplication {

	public static void main(String[] args) {
		SpringApplication.run(RestAppWeatherApplication.class, args);
	}

}
