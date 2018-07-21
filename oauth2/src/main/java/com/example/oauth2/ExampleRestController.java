package com.example.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.result.view.MustacheViewResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.config.ViewResolverRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.freemarker.FreeMarkerConfigurer;
import reactor.core.publisher.Mono;
import org.springframework.security.web.server.csrf.CsrfToken;

@RestController
@Slf4j
public class ExampleRestController {

    @Bean
    RouterFunction<?> routes() {
        return RouterFunctions
                .route(RequestPredicates.GET("/hello"),
                        r -> ServerResponse
                                .ok()
                                .body(r.principal()
                                                .repeat()
                                                .zipWith(
                                                        Mono.just("Hello "),
                                                        (pp, str) -> str + pp.getName()),
                                        String.class)

                );
    }

    @Bean
    RouterFunction<?> assetRoutes() {
        return RouterFunctions
                .resources("/images/**", new ClassPathResource("images/")
                );
    }

    @Bean
    RouterFunction<?> viewRoutes() {
        return RouterFunctions
                .route(RequestPredicates.GET("/index"),
                        r -> ServerResponse.ok().render("index")
                )
                .andRoute(RequestPredicates.GET("/form-login"),
                        r -> r.exchange()
                                .getAttributeOrDefault(
                                        CsrfToken.class.getName(),
                                        Mono.empty().ofType(CsrfToken.class)
                                ).flatMap(csrfToken -> {
//                                   r.exchange()
//                                    .getAttributes().put(csrfToken.getParameterName(), csrfToken);
                                    log.info("CSRFTOKEN: " + csrfToken.getToken() );
                                   r.attributes().put(csrfToken.getParameterName(), csrfToken);
                                   return ServerResponse.ok().render("form-login");
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
        registry.freeMarker();
    }

    // .. Configure Freemarker
    @Bean
    public FreeMarkerConfigurer freeMarkerConfigurer() {
        FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
        configurer.setTemplateLoaderPath("classpath:templates/freemarker");

        return configurer;
    }
}