package com.example.httpbasic;

import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

@RestController
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

}