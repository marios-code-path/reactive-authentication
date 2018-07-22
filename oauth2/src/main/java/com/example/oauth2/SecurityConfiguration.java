package com.example.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;

@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@Slf4j
@Configuration
public class SecurityConfiguration {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {

        return http
                .authorizeExchange()
                .pathMatchers("/form-login")
                .permitAll()
                .pathMatchers("/**")
                .hasRole("USER")
                .and()
                .formLogin()
                    .authenticationSuccessHandler(new RedirectServerAuthenticationSuccessHandler("/"))
                .and()
                .csrf()
                .and()
                .build();
    }
}
