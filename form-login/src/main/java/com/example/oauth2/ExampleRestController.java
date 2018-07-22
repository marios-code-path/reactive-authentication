package com.example.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.result.view.MustacheViewResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.config.ViewResolverRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Collections;

@RestController
@Slf4j
public class ExampleRestController {

    @Bean
    RouterFunction<?> assetRoutes() {
        return RouterFunctions
                .resources("/images/**", new ClassPathResource("images/")
                );
    }

    @Bean
    RouterFunction<?> viewRoutes() {
        return RouterFunctions
                .route(RequestPredicates.GET("/form-login"),
                        r -> r.exchange()
                                .getAttributeOrDefault(
                                        CsrfToken.class.getName(),
                                        Mono.empty().ofType(CsrfToken.class)
                                ).flatMap(csrfToken -> {
                                            r.exchange().getAttributes().put(csrfToken.getParameterName(), csrfToken);
                                            return ServerResponse
                                                    .ok()
                                                    .render("form-login",
                                                            r.exchange().getAttributes());
                                        }
                                )
                )
                .andRoute(RequestPredicates.GET("/"),
                        r -> r.principal()
                                .ofType(Authentication.class)
                                .flatMap(auth -> {
                                    User user = User.class.cast(auth.getPrincipal());
                                    return ServerResponse.ok().render("index",
                                            Collections.singletonMap("user", user));
                                })
                );
    }
}

@Configuration
class WebConfig implements WebFluxConfigurer {

    private final MustacheViewResolver resolver;

    // The resolver is provided by MustacheAutoConfiguration class
    WebConfig(MustacheViewResolver resolver) {
        this.resolver = resolver;
    }

    // order matters; cache will find first and render.
    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
        registry.viewResolver(resolver);
    }

}