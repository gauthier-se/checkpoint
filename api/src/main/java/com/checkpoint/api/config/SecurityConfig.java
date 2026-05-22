package com.checkpoint.api.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.checkpoint.api.security.ApiAuthenticationEntryPoint;
import com.checkpoint.api.security.JwtAuthenticationFilter;
import com.checkpoint.api.security.oauth2.CheckpointOAuth2UserService;
import com.checkpoint.api.security.oauth2.CheckpointOidcUserService;
import com.checkpoint.api.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.checkpoint.api.security.oauth2.OAuth2AuthenticationSuccessHandler;

/**
 * Security configuration providing two fully stateless filter chains:
 *
 * <ol>
 *   <li><strong>WebSocket chain</strong> ({@code /ws/**}): permits all HTTP upgrade
 *       requests; authentication is handled at the STOMP level. Evaluated first (order 0).</li>
 *   <li><strong>API chain</strong> ({@code /api/**}): stateless JWT authentication.
 *       Desktop clients send {@code Authorization: Bearer <token>};
 *       Web clients send the {@code checkpoint_token} HttpOnly cookie.
 *       CSRF is disabled (SameSite=Lax on the cookie prevents cross-site submission).
 *       Evaluated second (order 1).</li>
 * </ol>
 *
 * Public endpoints accessible without credentials:
 * <ul>
 *   <li>{@code /api/auth/**} — authentication endpoints</li>
 *   <li>{@code GET /api/games/**} — public game catalog</li>
 *   <li>{@code /error} — Spring Boot error endpoint</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;
    private final CheckpointOAuth2UserService checkpointOAuth2UserService;
    private final CheckpointOidcUserService checkpointOidcUserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2FailureHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          ApiAuthenticationEntryPoint apiAuthenticationEntryPoint,
                          CheckpointOAuth2UserService checkpointOAuth2UserService,
                          CheckpointOidcUserService checkpointOidcUserService,
                          OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler,
                          OAuth2AuthenticationFailureHandler oAuth2FailureHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.apiAuthenticationEntryPoint = apiAuthenticationEntryPoint;
        this.checkpointOAuth2UserService = checkpointOAuth2UserService;
        this.checkpointOidcUserService = checkpointOidcUserService;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
        this.oAuth2FailureHandler = oAuth2FailureHandler;
    }

    /**
     * Filter Chain 0 — WebSocket (HTTP upgrade handshake).
     * Matches all requests under {@code /ws/**}.
     * Permits all HTTP requests; actual authentication is handled at the
     * STOMP protocol level by {@link com.checkpoint.api.security.WebSocketAuthInterceptor}.
     */
    @Bean
    @Order(0)
    public SecurityFilterChain wsFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/ws/**")
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll())
                .build();
    }

    /**
     * Filter Chain 1 — API (stateless JWT).
     * Matches all requests under {@code /api/**}.
     * JWT is extracted from either the {@code Authorization: Bearer} header (Desktop)
     * or the {@code checkpoint_token} HttpOnly cookie (Web).
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http,
                                               ObjectProvider<ClientRegistrationRepository> oauth2Clients)
            throws Exception {
        HttpSecurity chain = http
                .securityMatcher("/api/**")
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/auth/ws-token").authenticated()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/oauth2/**").permitAll()
                        .requestMatchers("/api/login/oauth2/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/games/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/plays/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/genres").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/platforms").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/companies").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/members/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reviews/popular").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reviews/recent").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reviews/*/comments").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/comments/*/replies").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/lists/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/leaderboard/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/news/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(apiAuthenticationEntryPoint))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        if (oauth2Clients.getIfAvailable() != null) {
            chain.oauth2Login(oauth2 -> oauth2
                    .authorizationEndpoint(endpoint -> endpoint
                            .baseUri("/api/oauth2/authorization"))
                    .redirectionEndpoint(endpoint -> endpoint
                            .baseUri("/api/login/oauth2/code/*"))
                    .userInfoEndpoint(endpoint -> endpoint
                            .userService(checkpointOAuth2UserService)
                            .oidcUserService(checkpointOidcUserService))
                    .successHandler(oAuth2SuccessHandler)
                    .failureHandler(oAuth2FailureHandler));
        }

        return chain.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
