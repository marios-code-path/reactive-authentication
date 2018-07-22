package com.example.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AnonymousAuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.ReactiveAuthenticationManagerAdapter;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;

import java.util.Arrays;
import java.util.Collections;

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


