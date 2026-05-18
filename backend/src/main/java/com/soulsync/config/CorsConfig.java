package com.soulsync.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS configuration.
 *
 * We use setAllowedOriginPatterns("*") rather than setAllowedOrigins("*") so
 * that Spring can still echo back the exact request Origin in the response
 * header (required when allowCredentials=true).  Security comes from JWT
 * validation — not from origin restriction — so allowing all origins is safe
 * for this stateless, cookie-free API.
 *
 * To restrict origins in a high-security deployment set:
 *   APP_CORS_ALLOWED_ORIGINS=https://yourapp.vercel.app,https://custom.domain
 * When that env var is present the wildcard pattern is replaced with the
 * explicit list.
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:}")
    private String allowedOriginsEnv;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        if (allowedOriginsEnv != null && !allowedOriginsEnv.isBlank()) {
            // Explicit origin list provided — use exact matching
            List<String> origins = List.of(allowedOriginsEnv.split(","))
                    .stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
            config.setAllowedOrigins(origins);
        } else {
            // No restriction — allow any origin (safe: auth via JWT, no cookies)
            config.setAllowedOriginPatterns(List.of("*"));
        }

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
