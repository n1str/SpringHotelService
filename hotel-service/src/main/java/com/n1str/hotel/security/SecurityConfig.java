package com.n1str.hotel.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.n1str.hotel.dto.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.UUID;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            
            ErrorResponse errorResponse = new ErrorResponse(
                    LocalDateTime.now(),
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Unauthorized",
                    "Authentication required - Missing or invalid JWT token",
                    request.getRequestURI(),
                    UUID.randomUUID().toString()
            );
            
            // Создаём ObjectMapper с поддержкой JSR310 для сериализации LocalDateTime
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            
            response.getWriter().write(mapper.writeValueAsString(errorResponse));
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz -> authz
                    // Эндпойнты Swagger и OpenAPI - доступны без авторизации
                    .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/swagger-ui/index.html",
                            "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()
                    // Консоль H2 - доступна без авторизации
                    .requestMatchers("/h2-console/**").permitAll()
                    
                    // Внутренние эндпойнты для обслуживания сервис-сервис (без авторизации)
                    .requestMatchers(HttpMethod.POST, "/api/rooms/*/confirm-availability").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/rooms/*/release").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/rooms/*/increment-booking").permitAll()
                    
                    // Защищённые эндпойнты - требуется авторизация
                    .requestMatchers(HttpMethod.GET, "/api/**").authenticated()
                    .requestMatchers(HttpMethod.POST, "/api/**").authenticated()
                    .requestMatchers(HttpMethod.PATCH, "/api/**").authenticated()
                    .requestMatchers(HttpMethod.DELETE, "/api/**").authenticated()
                    .anyRequest().authenticated()
            )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint())
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        // Настройки для консоли H2 (позволяет загружаться в iframe)
        http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }
}

