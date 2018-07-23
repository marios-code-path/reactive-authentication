package com.example.formlogin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.reactive.config.EnableWebFlux;

import java.util.Properties;

@SpringBootApplication
@EnableWebFlux
public class FormLoginApp {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(FormLoginApp.class);
        app.run();
    }
}
