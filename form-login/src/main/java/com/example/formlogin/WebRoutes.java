package com.example.formlogin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.result.view.MustacheViewResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.config.ViewResolverRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Collections;

@Component
@Slf4j
public class WebRoutes {

    @Bean
    RouterFunction<?> iconResources() {
        return RouterFunctions
                .resources("/favicon**", new ClassPathResource("images/favicon.ico"));
    }


    @Bean
    RouterFunction<?> assetRoutes() {
        return RouterFunctions
                .resources("/images/**", new ClassPathResource("images/"));
    }

    @Bean
    RouterFunction<?> viewRoutes() {
        return RouterFunctions
                .route(RequestPredicates.GET("/login"),
                        r -> r.exchange()
                                .getAttributeOrDefault(
                                        CsrfToken.class.getName(),
                                        Mono.empty().ofType(CsrfToken.class)
                                ).flatMap(csrfToken -> {
                                            r.exchange().getAttributes().put(csrfToken.getParameterName(), csrfToken);
                                            return ServerResponse
                                                    .ok()
                                                    .render("login-form",
                                                            r.exchange().getAttributes());
                                        }
                                )
                )
                .andRoute(RequestPredicates.GET("/"),
                        r -> r.principal()
                                .ofType(Authentication.class)
                                .flatMap(auth -> {
                                    User user = User.class.cast(auth.getPrincipal());
                                    r.exchange()
                                            .getAttributes()
                                            .putAll(Collections.singletonMap("user", user));
                                    return ServerResponse.ok().render("index",
                                            r.exchange().getAttributes());
                                })
                )
                .andRoute(RequestPredicates.GET("/bye"),
                        r -> ServerResponse.ok().render("bye")
                )
                .filter((q, r) ->
                        q.exchange()
                                .getAttributeOrDefault(
                                        CsrfToken.class.getName(),
                                        Mono.empty().ofType(CsrfToken.class)
                                )
                                .flatMap(csrfToken -> {
                                    q.exchange()
                                            .getAttributes()
                                            .put(csrfToken.getParameterName(), csrfToken);
                                    return r.handle(q);
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