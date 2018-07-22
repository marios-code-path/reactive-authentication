package com.example.formlogin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.reactive.config.EnableWebFlux;

import java.util.Properties;

@SpringBootApplication
@EnableWebFlux
public class Oauth2Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Oauth2Application.class);
        Properties props = new Properties();
        props.setProperty("spring.mustache.expose-request-attributes",
                Boolean.TRUE.toString());
        app.setDefaultProperties(props);

        app.run();
    }
}
