package com.n1str.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Configuration
public class GatewayConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOrigins(Arrays.asList("*"));
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        corsConfig.setAllowedHeaders(Arrays.asList("*"));
        corsConfig.setAllowCredentials(false);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        
        return new CorsWebFilter(source);
    }
}

@Component
@Slf4j
class CorrelationIdGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            // Получаем или создаём ID корреляции для трассировки запроса
            String correlationId = exchange.getRequest().getHeaders().getFirst("X-Correlation-Id");
            if (correlationId == null || correlationId.isEmpty()) {
                correlationId = UUID.randomUUID().toString();
            }
            
            final String finalCorrelationId = correlationId;
            
            exchange.getAttributes().put("X-Correlation-Id", finalCorrelationId);
            
            return chain.filter(
                exchange.mutate()
                    .request(exchange.getRequest().mutate()
                        .header("X-Correlation-Id", finalCorrelationId)
                        .build())
                    .build()
            ).doFinally(signalType -> {
                log.debug("Запрос завершён с X-Correlation-Id: {}", finalCorrelationId);
            });
        };
    }
}


@Component
@Slf4j
class JwtValidationGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {
    
    @Value("${jwt.secret:my-secret-key-for-jwt-token-generation-at-least-256-bits-long-for-HS256-algorithm}")
    private String jwtSecret;

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getPath().value();
            
            // Пропускаем JWT валидацию для публичных эндпойнтов
            if (isPublicEndpoint(path)) {
                log.debug("[path:{}] Публичный эндпойнт - пропускаем JWT валидацию", path);
                return chain.filter(exchange);
            }
            
            String token = extractToken(exchange.getRequest().getHeaders().getFirst("Authorization"));
            
            if (token == null) {
                log.warn("[path:{}] JWT токен отсутствует", path);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().writeWith(
                    Mono.just(exchange.getResponse()
                        .bufferFactory()
                        .wrap(createErrorResponse("JWT токен отсутствует", 401, path)))
                );
            }
            
            log.debug("[path:{}] JWT токен найден, пробрасываем в сервис для полной валидации", path);
            // JWT валидируется в сервисах (более безопасный подход)
            return chain.filter(exchange);
        };
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }

    private boolean isPublicEndpoint(String path) {
        return path.contains("/user/auth") || 
               path.contains("/user/register") ||
               path.contains("/swagger-ui") ||
               path.contains("/v3/api-docs") ||
               path.contains("/h2-console");
    }

    private byte[] createErrorResponse(String message, int status, String path) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("timestamp", LocalDateTime.now());
            response.put("status", status);
            response.put("error", "Unauthorized");
            response.put("message", message);
            response.put("path", path);
            
            // Ручная сборка JSON чтобы избежать зависимости от ObjectMapper
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"timestamp\":\"").append(response.get("timestamp")).append("\",");
            json.append("\"status\":").append(status).append(",");
            json.append("\"error\":\"").append(response.get("error")).append("\",");
            json.append("\"message\":\"").append(message).append("\",");
            json.append("\"path\":\"").append(path).append("\"");
            json.append("}");
            
            return json.toString().getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            return ("{\"error\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        }
    }
}

