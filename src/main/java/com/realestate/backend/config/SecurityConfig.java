package com.realestate.backend.config;

import com.realestate.backend.security.PermissionAuthorizationFilter;
import com.realestate.backend.security.jwt.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

import java.util.List;

/**
 * SecurityConfig — Spring Security configuration.
 *
 * With the permission-based system, SecurityConfig ONLY:
 *   1. Defines public paths (without authentication)
 *   2. Requires authentication for everything else
 *
 * Authorization (what each user is allowed to do) is handled entirely by:
 *   → PermissionAuthorizationFilter (middleware)
 *   → Based on DB: user_roles → role_permissions → permissions
 *
 * No hasRole(), no hasAnyRole(), no hardcoded rules.
 * Adding new roles and permissions does not require code changes.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter                  jwtAuthFilter;
    private final PermissionAuthorizationFilter  permissionFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/api/auth/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/actuator/health",
                                "/uploads/**"
                        ).permitAll()

                        .anyRequest().authenticated()
                )

                .addFilterBefore(jwtAuthFilter,     UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(permissionFilter,   JwtAuthFilter.class)

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, authEx) -> {
                            res.setStatus(401);
                            res.setContentType("application/json");
                            res.getWriter().write(
                                    "{\"error\":\"Unauthorized\",\"message\":\"Authentication required\"}"
                            );
                        })
                        .accessDeniedHandler((req, res, accessEx) -> {
                            res.setStatus(403);
                            res.setContentType("application/json");
                            res.getWriter().write(
                                    "{\"error\":\"Forbidden\",\"message\":\"Insufficient permissions\"}"
                            );
                        })
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173",
                "https://yourdomain.com"
        ));
        config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}