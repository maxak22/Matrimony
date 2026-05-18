package com.soulsync.config;

import com.soulsync.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CorsConfig corsConfig;
    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(CorsConfig corsConfig, JwtAuthFilter jwtAuthFilter) {
        this.corsConfig = corsConfig;
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers -> headers
                .contentTypeOptions(cto -> {})
                .frameOptions(fo -> fo.deny())
                .referrerPolicy(ref ->
                    ref.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; " +
                    "img-src 'self' https://res.cloudinary.com https://images.unsplash.com data:; " +
                    "font-src 'self' https://fonts.gstatic.com; " +
                    "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                    "script-src 'self'; " +
                    "frame-ancestors 'none'"
                ))
            )
            .authorizeHttpRequests(auth -> auth
                // CORS pre-flight requests must never be blocked by Spring Security
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // WebSocket handshake (JWT validated by WebSocketAuthInterceptor on STOMP CONNECT)
                .requestMatchers("/ws/**").permitAll()
                // Public auth endpoints
                .requestMatchers(
                    "/api/auth/login",
                    "/api/auth/verify-otp",
                    "/api/auth/resend-otp",
                    "/api/auth/health"
                ).permitAll()
                // Registration
                .requestMatchers(HttpMethod.POST, "/api/profiles").permitAll()
                // Photo upload — needed during registration before JWT exists
                .requestMatchers("/api/upload/**").permitAll()
                // Public success stories (landing page)
                .requestMatchers(HttpMethod.GET, "/api/stories/**").permitAll()
                // Everything else requires a valid JWT
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
