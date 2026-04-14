package com.realestate.backend.config;

import com.realestate.backend.security.jwt.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)

                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth

                        // PUBLIC
                        .requestMatchers(
                                "/api/auth/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/actuator/health"
                        ).permitAll()

                        // PROPERTIES
                        .requestMatchers(HttpMethod.GET, "/api/properties/**")
                        .hasAnyRole("ADMIN", "AGENT", "CLIENT")

                        .requestMatchers(HttpMethod.POST, "/api/properties")
                        .hasAnyRole("ADMIN", "AGENT")

                        .requestMatchers(HttpMethod.PUT, "/api/properties/**")
                        .hasAnyRole("ADMIN", "AGENT")

                        .requestMatchers(HttpMethod.DELETE, "/api/properties/**")
                        .hasRole("ADMIN")

                        // USERS
                        .requestMatchers("/api/users/**").hasAnyRole("ADMIN", "AGENT")

                        // LEADS
                        .requestMatchers(HttpMethod.POST, "/api/leads")
                        .hasAnyRole("ADMIN", "AGENT", "CLIENT")

                        .requestMatchers(HttpMethod.GET, "/api/leads/**")
                        .hasAnyRole("ADMIN", "AGENT")

                        // CONTRACTS + PAYMENTS
                        .requestMatchers("/api/contracts/**").hasAnyRole("ADMIN", "AGENT")
                        .requestMatchers("/api/payments/**").hasAnyRole("ADMIN", "AGENT")

                        // AI
                        .requestMatchers("/api/ai/**").hasAnyRole("ADMIN", "AGENT", "CLIENT")

                        // ADMIN
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        .anyRequest().fullyAuthenticated()
                )

                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class).exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, authEx) -> {
                            res.setStatus(401);
                            res.setContentType("application/json");
                            res.getWriter().write("{\"error\":\"Unauthorized\"}");
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