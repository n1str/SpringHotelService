package com.n1str.booking.config;

import feign.Logger;
import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
@Slf4j
public class FeignAuthConfig {

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    public RequestInterceptor authForwardingRequestInterceptor() {
        return template -> {
            try {
                ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs != null) {
                    String authorization = attrs.getRequest().getHeader("Authorization");
                    if (authorization != null && !authorization.isBlank()) {
                        template.header("Authorization", authorization);
                        log.debug("Authorization header forwarded to Feign request");
                    }
                    String traceId = attrs.getRequest().getHeader("X-Trace-Id");
                    if (traceId != null && !traceId.isBlank()) {
                        template.header("X-Trace-Id", traceId);
                    }
                } else {
                    log.debug("Нет HTTP контекста для Feign запроса - обнаружен вызов сервис-сервис");
                }
            } catch (IllegalStateException e) {
                log.debug("RequestContextHolder недоступен, пропускаем пересылку заголовков: {}", e.getMessage());
            }
        };
    }
}


