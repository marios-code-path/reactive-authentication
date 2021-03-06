package com.example.httpbasic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.reactive.config.EnableWebFlux;

@SpringBootApplication
@EnableWebFlux
public class HttpBasicApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(HttpBasicApplication.class);
		app.setAdditionalProfiles("custom");
		app.run(args);
	}
}
