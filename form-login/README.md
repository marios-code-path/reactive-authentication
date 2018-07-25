+++
date = 2018-07-21
publishDate = 2018-07-27
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

Next, we can open all of the public-facing endpoints by using a PathMatcher to match our public routes, by applying permitAll to open permissions there.

SecurityConfiguration.java:
            return http
                    .authorizeExchange()
                    .pathMatchers("/login",
                            "/bye",
                            "/favicon.ico",
                            "/images/**")
                    .permitAll()

For this example, we want every endpoint thats not publicly available to require a logged-in user. Wire in a PathMatcher for all routes, and apply the authenticated() operator to whatever matches. 

### CSRF Configuration

Another component for when configuring SecurityWebFilterChain is the [CsrfSpec](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/config/web/server/ServerHttpSecurity.CsrfSpec.html) enabled by calling `csrf()` method.  This lets us configure [CSRF](https://www.owasp.org/index.php/Cross-Site_Request_Forgery_(CSRF)_Prevention_Cheat_Sheet) tokens and handlers, or exclude CSRF entirely.

To configure CSRF metadata behaviour, create a bean of type [ServerCsrfTokenRepository](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/web/server/csrf/ServerCsrfTokenRepository.html) and set header and/or parameter attrubte names as needed. In this demo we will use the default options, so '_csrf' becomes our parameter namd, and 'X-CSRF-TOKEN' is our header.

SecurityConfiguration.java:
                    .pathMatchers("/**")
                    .authenticated()
                    .and()
                    .csrf()

Spring Security provides login/logout pages on demand whenever one is not already configured. This is provided by the LoginPageGeneratingWebFilter and LogoutPageGeneratingWebFilter that get wired in if no login/logout page was specified.

To override this, we can issue loginPage("/") and it's success/error Handlers within the FormLoginSpec. We want to redirect successes to the home page by using RedirectServerAuthenticationSuccessHandler.  Then For logout successes, we'll send the user to the "/bye" endpoint by configuring the RedirectServerLogoutSuccessHandler in a separate method since it's contructor doesnt support parameters.

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

Both handlers do somilar things - namely redirect on success. Its jsut that we have to expelicitly construct the logoutSuccessHandler since it's constructor is no-args.

### Expressing Users

The [UserDetailsRepositoryReactiveAuthenticationManager](https://docs.spring.io/spring-security/site/docs/5.0.3.RELEASE/api/org/springframework/security/authentication/UserDetailsRepositoryReactiveAuthenticationManager.html)
bean is provided automatically if there are no other configured [ReactiveAuthenticationManager](http://ReactiveAuthenticationManager) `@Bean` definitions. This authentication manager defers principal/credential operations to a [ReactiveUserDetailsService](https://docs.spring.io/spring-security/site/docs/5.1.0.M1/api/org/springframework/security/core/userdetails/ReactiveUserDetailsService.html) implementation.

Spring comes with ready-made implemenations for storing and looking up users in the [MapReactiveUserDetailsService](http://MapReactiveUserDetailsService). We'll complete this section using the map reactive implementation, and by having our users come from the handly `User` object since no other details are necessary for [customizing the user](http://link-to-http-basic-article).

UserDetailBeans.java:
    @Configuration
    public class UserDetailServiceBeans {

        @Bean
        public MapReactiveUserDetailsService mapReactiveUserDetailsService() {
            return new MapReactiveUserDetailsService(users);
        }

        private static final PasswordEncoder pw =     PasswordEncoderFactories.createDelegatingPasswordEncoder();

        private static UserDetails user(String u, String... roles) {

            return User
                    .withUsername(u)
                    .passwordEncoder(pw::encode)
                    .password("pw")
                    .authorities(roles)
                    .build();
        }

        private static final Collection<UserDetails> users = new ArrayList<>(
                Arrays.asList(
                        user("thor", "ROLE_USER"),
                        user("loki", "ROLE_USER"),
                        user("odin", "ROLE_ADMIN", "ROLE_USER")
                ));
    }

## An example website

The website uses just 2 pages to describe a game. The game text is tanken from Infocom's Zork text-based adventure. In this demo we will have a user login, and finally do something to escalate permission. We will walk throuigh componentry needed to accomplish this task. Spring Security provides a reactive way for describing security constraints within your web app.  

Lets take a look at how we want thise site to get rendered. First lets take a look at WebFlux MVC which lets us expose mustache views. We have a entry view we want to surface, so by creating these templates, we have a full view complement.

Mustache lets us use includes for re-usable content. We will create 3 common fragements for our views.

frag/header.html:
    <!doctype html>
    <html lang="en">
    <body>

frag/footer.html:
    </body>
    </html>

frag/logout.html:
    <form class="form-inline" action="/logout" method="post">
        <input type="hidden" name="_csrf" value="{{_csrf.token}}" />
        <button class="btn btn-lg btn-primary btn-block" type="submit">Escape!</button>
    </form>

Next we can define the meat of our content. We just need a login, game, and logout page.

index.html:
    {{>frag/header}}
    <h1>West of House</h1>
    <div>Hello, {{user.username}}. You are standing in an open field west of a white house, with a boarded front door.<br/>
        There is a small mailbox here.</div>
    <br/>
    &gt;<image src="/images/cursor.gif" />
    {{>frag/logout}}
    {{>frag/footer}}

login-form.html:
    {{>frag/header}}
    <h1>Welcome to Sp0rk. This version created 21-JUL-2018</h1>
    <div id="main-content" class="container">
        <div class="row">
            <div class="col-md-6">
                <form class="form-inline" action="/login" method="post">
                    <div class="form-group">
                        <label for="username">What is your name?</label>
                        <input type="text" name="username" id="username" class="form-control" placeholder="My name is...">
                    </div>
                    <div class="form-group">
                        <label for="password">What is the passcode?</label>
                        <input type="password" name="password" id="password" class="form-control" placeholder="Passphrase">
                    </div>
                    <br/>
                    <input type="hidden" name="_csrf" value="{{_csrf.token}}" />
                    <button class="btn btn-lg btn-primary btn-block" type="submit">Who R U</button>
                </form>
            </div>
        </div>
    </div>
    {{>frag/footer}}

bye.html:
    {{>frag/header}}
    <h1>Leaving the Great Underground Empire</h1>
    <div>
        See you later, dungeon-master!<br/>
    </div>
    <br/>
    {{>frag/footer}}

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

