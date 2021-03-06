package com.example.httpbasic;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.config.ViewResolverRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.view.freemarker.FreeMarkerConfigurer;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;


@Configuration
public class UserDetailServiceBeans {

    private static final PasswordEncoder pw = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    private static UserDetails user(String u, String... roles) {
        return new ExampleUser(new ExampleUser.Account(u, pw.encode("password"), true),
                roles);
    }

    private static final Collection<UserDetails> users = new ArrayList<>(
            Arrays.asList(
                    user("thor", "ROLE_ADMIN"),
                    user("loki", "ROLE_USER"),
                    user("zeus", "ROLE_ADMIN", "ROLE_USER")
            ));

    @Bean
    @Profile("map-reactive")
    public MapReactiveUserDetailsService mapReactiveUserDetailsService() {
        return new MapReactiveUserDetailsService(users);
    }

    @Component
    @Profile("custom")
    class ExampleUserDetailService implements ReactiveUserDetailsService {

        @Override
        public Mono<UserDetails> findByUsername(String username) {
            Optional<UserDetails> maybeUser = users.stream().filter(u -> u.getUsername().equalsIgnoreCase(username)).findFirst();
            return maybeUser.map(Mono::just).orElse(Mono.empty());
        }
    }

}

class WebConfig implements WebFluxConfigurer {
    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
        registry.freeMarker();
    }

    // Configure Freemarker...

    @Bean
    public FreeMarkerConfigurer freeMarkerConfigurer() {
        FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
        configurer.setTemplateLoaderPath("classpath:/templates");
        return configurer;
    }
}