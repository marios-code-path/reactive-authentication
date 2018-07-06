
# Reactive Spring Security Authentication

Getting into concepts behind Spring Security Reactive's Authentication components. We will discover the direction needed to make exposing your custom user domain simple, and understandable. Spring comes pre-packed with a good coverage of use-cases (specifically in the username/password flows), as we can solve some additional authentication issues by re-using these components.

Spring Authentication requires detailed knowledge of the underlaying authentication protocol at work. We may need to authenticate using a login form (HTTP BASIC) or a token exchange (oAuth2). Lets take a look at a username/password flow, so we can explore the inner-workings in detail for the oAuth2 flow later.

## Authentication flow-control

How do we determine when a request must provide an authenticated context? Spring does this with help from an [AuthenticationEntryPoint](https://docs.spring.io/spring-security/site/docs/5.0.0.M3/api/org/springframework/security/web/server/AuthenticationEntryPoint.html)
that converts eligible requests into a possible authentication response by means of headers, status code, form login, etc..

Configure [ServerHttpSecurity](http://foo-bar) to use HTTP-BASIC by calling it's `httpBasic()` method. This will auto-configure handlers, and expose [HttpBasicSpec](http://foo-bar)'s lower level components such as the [ReactiveAuthenticationManager](http://foo-bar). For now, we are interested in overriding the default [HttpBasicServerAuthenticationEntryPoint](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/web/server/authentication/HttpBasicServerAuthenticationEntryPoint.html) it provides. This entry-point responds to un-authenticated requests with `WWW-Authenticate` headers and status 401, triggering the HTTP-Basic login interaction.

We can customize this behaviour via the [ExceptionHandlingSpec](http://foo-bar). To demonstrate, we can override the provided [HttpBasicServerAuthenticationEntryPoint](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/web/server/authentication/HttpBasicServerAuthenticationEntryPoint.html) with an [RedirectServerAuthenticationEntryPoint](http://foo-bar) that redirects users to the "/custom-login" view.

NOTE: I do not condon the muddling of authentication flows; this is just an example:

SecurityConfiguration.java:

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {

        return http
                .authorizeExchange()
                ...
                .and()
                .exceptionHandling()
                    .authenticationEntryPoint(new RedirectServerAuthenticationEntryPoint("/form-login"))
                .and()
                .httpBasic()
                .and()
                .build();
    }

### Highlighting Authentication flow-control paths

[AuthenticationEntryPoint](https://docs.spring.io/spring-security/site/docs/5.0.0.M3/api/org/springframework/security/web/server/AuthenticationEntryPoint.html) is activated when an un-authenticated request raises an [AccessDeniedException](http://flow-control). The exception (by default) is caught within [ExceptionTranslationWebFilter](http://ExceptionTranslationWebFilter) to either deny access completely in case of an un-authorized user, or by calling [AuthenticationEntryPoint](https://docs.spring.io/spring-security/site/docs/5.0.0.M3/api/org/springframework/security/web/server/AuthenticationEntryPoint.html)'s `commence()` method to initiate authentication flow.

Additionally, we can alter the access-denial behaviour as well providing an [ServerAccessDeniedHandler](http://ServerAcessDeniedHandler) to [ExceptionHandlingSpec](http://foo-bar)'s `accessDeniedHandler()` method.

## Authentication Mechanisms

AuthenticationManager is the interface that manages the authentication flow details - roles, username, credential, IP, etc.. -. Example is UsernamePasswordAuthenticationToken used for processing simple username/password credentials.of special note is method `isAuthenticated()` which tells Spring Security whether it should authenticate a user (e.g. HTTP basic authenticate), or pass the request along the filter chain to complete the original request.

This UsernamePasswordAuthenticationToken is created 2 times; pre-auth where the AuthenticationManager receives a interactive credential from the user for authenticating, and thus `isAuthenticated()` is `false`. The other time we see this instance is on authentication success, where its populated with principal details, and `isAuthenticated()` return `true`.

A WebFilterExchange will contain both the ServerWebExchange, and the WebFilterChain.

[ReactiveAuthenticationManager](https://docs.spring.io/spring-security/site/docs/5.0.x/api/org/springframework/security/authentication/ReactiveAuthenticationManager.html)
does the job of facilitating authentication mechanisms - e.g. HTTP/BASIC which is included automatically- in your web application.

NOTE: Spring provides an integration component [ReactiveAuthenticationManagerAdapter](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/authentication/ReactiveAuthenticationManagerAdapter.html)
for hoisting your existing, classic AuthenticationManager implementations into the reactive world.

### Custom Domain Users

The [UserDetailsRepositoryReactiveAuthenticationManager](https://docs.spring.io/spring-security/site/docs/5.0.3.RELEASE/api/org/springframework/security/authentication/UserDetailsRepositoryReactiveAuthenticationManager.html)
bean is provided automatically if there are no other configured ReactiveAuthenticationManagers `@Bean` definitions. This authentication manager defers principal/credential operations to a [ReactiveUserDetailsService](https://docs.spring.io/spring-security/site/docs/5.1.0.M1/api/org/springframework/security/core/userdetails/ReactiveUserDetailsService.html).

Spring comes with ready-made implemenations for storing and looking up users in the MapReactiveUserDetailsService - simple for demos - but we want to go a little in depth.  We'll complete this section by making 2 uses of this bean - one MapReactive, the other our own - to illustrate simplicity in overriding and levering this component..

First, the custom User domain object which implements UserDetails as prescribed by the UserDetailsService interface:

ExampleUser.java:

    @Data
    @Slf4j
    public class ExampleUser implements UserDetails {

        private final Account account;
        Collection<GrantedAuthority> authorities;

        public ExampleUser(Account account, String[] roles) {
            this.authorities = Arrays.asList(roles)
                    .stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
            this.account = account;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return this.authorities;
        }

        @Override
        public String getPassword() {
            return account.getPassword();
        }

        @Override
        public String getUsername() {
            return account.getUsername();
        }

        @Override
        public boolean isAccountNonExpired() {
            return account.isActive();
        }

        @Override
        public boolean isAccountNonLocked() {
            return account.isActive();
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return account.isActive();
        }

        @Override
        public boolean isEnabled() {
            return account.isActive();
        }

        @Data
        public static class Account {

            private String username;
            private String password;
            private boolean active;

            public Account(String username, String password, boolean active) {
                this.username = username;
                this.password = password;
                this.active = active;
            }

        }
    }

We need to provide a way to get our users out of a user service, in this demo we will use a pre-programmed List() of users to hold any UserDetails we want to expose through the app. We provide a few convenicne methods to setting up the object. Of significant import is the [PasswordEncoder](https://docs.spring.io/spring-security/site/docs/4.2.4.RELEASE/apidocs/org/springframework/security/crypto/password/PasswordEncoder.html) that is used to encrypt/encode (defaults to bcrypt) plaintext.

UserDetailServiceBean.java:

    @Configuration
    public class UserDetailServiceBeans {

        private static final PasswordEncoder pw = PasswordEncoderFactories.createDelegatingPasswordEncoder();

        private static UserDetails user(String u, String... roles) {
            return new ExampleUser(new ExampleUser.Account(u, pw.encode("password"), true),
                    roles);
        }

        private static final Collection<UserDetails> users = new    ArrayList<>(
            Arrays.asList(
                    user("thor", "ROLE_ADMIN"),
                    user("loki", "ROLE_USER"),
                    user("zeus", "ROLE_ADMIN", "ROLE_USER")
            ));
    //...

Now, with users available, we can wire in a UserDetailService. Lets start with the easy-to-use MapReactiveUserDetailService. We'll bind it to a spring profile "map-reactive" for use case demonstration.

UserDetailServiceBean.java:

    @Bean
    @Profile("map-reactive")
    public MapReactiveUserDetailsService mapReactiveUserDetailsService() {
        return new MapReactiveUserDetailsService(users);
    }

Because we have satisified all of the core componetns needed ot lock down our applicaiton, We can actually stop here with configuration.

On the other hand, what if I wanted to implement my own ReactiveUserDetailService? This can be accomplished! simply wire in an own implementation of ReactiveUserDetailsService as a bean. We'll bind it ot the spring profile "custom" for use case demonstration.

UserDetailServiceBeans.java:

    @Component
    @Profile("custom")
    class ExampleUserDetailService implements ReactiveUserDetailsService {

        @Override
        public Mono<UserDetails> findByUsername(String username) {
            Optional<UserDetails> maybeUser = users.stream().filter(u -> u.getUsername().equalsIgnoreCase(username)).findFirst();
            return maybeUser.map(Mono::just).orElse(Mono.empty());
        }
    }


