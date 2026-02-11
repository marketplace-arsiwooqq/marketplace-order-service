package com.arsiwooqq.orderservice.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignAuthConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            var attributes = RequestContextHolder.getRequestAttributes();
            if (attributes instanceof ServletRequestAttributes requestAttributes) {
                HttpServletRequest request = requestAttributes.getRequest();
                Object jwt = request.getAttribute("jwt");
                if (jwt instanceof String) {
                    template.header("Authorization", "Bearer " + jwt);
                }
            }
        };
    }
}
