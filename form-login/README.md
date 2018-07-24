+++
date = 2018-07-21
publishDate = 2018-07-26
title = "Setup and Customize a Login Page With Reactive Spring Security."
description = "Spring Security provides a intuitive and concise API for managing Authentication aspects within your app."
toc = true
categories = ["appsec","security","reactive"]
tags = ["functional","java","spring","web","demo"]
+++

# Configuring Authentication against a WebFlux app

This demonstration examines Spring Security WebFlux's Authentication mechanisms. We will look at authentication with HTML forms using Mustache, http-basic login, and customized logout configurations.

## A ServerHttpSecurity Configuration

Normally, start with the website specifics, but we will begin with security configuration since this is a security-related article. With the @EnableWebFluxSecurity on, we can build the SecurityWebFilterChain by issuing commands to the ServerHttpSecurity DSL object.


SecurityConfiguration.java:
    @EnableWebFluxSecurity
    @Slf4j
    @Configuration
    public class SecurityConfiguration {

        @Bean
        public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {

Next, we can open all of the public-facing endpoints by using a PathMatcher to match our public routes, and then applying permitAll to open permissions there.

SecurityConfiguration.java:
            return http
                    .authorizeExchange()
                    .pathMatchers("/login",
                            "/bye",
                            "/favicon.ico",
                            "/images/**")
                    .permitAll()

For this example, we want every endpoint thats not publicly available to require a logged-in user. Wire in a PathMatcher for all routes, and apply the authenticated() operator to whatever matches. Then we can configure CSRF for handling our '_csrf' token state between requests. We will tackle the specifics for exposing the CSRF token, in the route/handler section.

SecurityConfiguration.java:
                    .pathMatchers("/**")
                    .authenticated()
                    .and()
                    .csrf()

SecurityConfiguration.java:


                    .and()
                    .formLogin()
                        .loginPage("/login")
                        .authenticationSuccessHandler(new RedirectServerAuthenticationSuccessHandler("/"))
                    .and()
                    .logout()
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(logoutSuccessHandler("/bye"))
                    .and()
                    .build();
        }

        public ServerLogoutSuccessHandler logoutSuccessHandler(String uri) {
            RedirectServerLogoutSuccessHandler successHandler = new RedirectServerLogoutSuccessHandler();
            successHandler.setLogoutSuccessUrl(URI.create(uri));
            return successHandler;
        }

    }

## An example website

The website uses just 2 pages to describe a game. The game text is tanken from Infocom's Zork text-based adventure. In this demo we will have a user login, and finally do something to scalate permission. We will walk throuigh componentry needed to accomplish this task. Spring Security provides a reacgtve and concise way for describing security constraints within your web app.  

Lets take a look at how we want thise site to get rendered. First lets take a look at WebFlux MVC which lets us expose mustache views. We have a entry view we want to surface, so by creating these templates, we have a full view complement.

index.html:
    html goes here

login-form.html:
    html goes  here

logout.html:
    html goes here

bye.html:
    html goes here

Lets configure the class into Spring WebFlux that lets us return mustache views. We can add MustacheViewResolver to our WebFlux Configuration. This makes it so that the viewResolver selects MustacheView's as the type compiled and shown on screen.

WebConfig.java:
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

# Routing to views

We need to wire up our views with routing logic, so lets add this with the functional style RouterFunction.

We need a way to display icons, so first we will wire in a 'favicon.ico' route, and send it to a ClassPath resource for file resolution.


WebRoutes.java:
    @Component
    public class WebRoutes {

        @Bean
        RouterFunction<?> iconResources() {
            return RouterFunctions
                    .resources("/favicon.**", new ClassPathResource("images/favicon.ico"));
        }

WebRoutes.java:
    @Bean
    RouterFunction<?> viewRoutes() {
        return RouterFunctions
                .route(RequestPredicates.GET("/login"),
                        req -> ServerResponse
                                .ok()
                                .render("login-form",
                                        req.exchange().getAttributes())

                )


## Types of Routing

Next, lets add the route to our favicon.ico. We can use a .resources function in RouterFunctions that exposes us FileSystem and ClassPath resources for a given route.


## Filtering views

## Public Views

# Securing the application

SecurityConfiguration.java:
    @EnableWebFluxSecurity
    @EnableReactiveMethodSecurity
    @Slf4j
    @Configuration
    public class SecurityConfiguration {

        @Bean
        public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {

            return http
                    .authorizeExchange()
                    .pathMatchers("/login",
                            "/bye",
                            "/favicon.ico",
                            "/images/**")
                    .permitAll()
                    .pathMatchers("/**")
                    .hasRole("USER")
                    .and()
                    .formLogin()
                        .loginPage("/login")
                        .authenticationSuccessHandler(new RedirectServerAuthenticationSuccessHandler("/"))
                    .and()
                    .logout()
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(logoutSuccessHandler("/bye"))
                    .and()
                    .csrf()
                    .and()
                    .build();
        }

        public ServerLogoutSuccessHandler logoutSuccessHandler(String uri) {
            RedirectServerLogoutSuccessHandler successHandler = new RedirectServerLogoutSuccessHandler();
            successHandler.setLogoutSuccessUrl(URI.create(uri));
            return successHandler;

        }
    }

## public views

## authorized views

## login/logout views

## implementation and example

