package com.arsiwooqq.orderservice.client;

import com.arsiwooqq.orderservice.dto.ApiResponse;
import com.arsiwooqq.orderservice.dto.UserResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "${USER_SERVICE_URL:}")
public interface UserServiceClient {
    @GetMapping("/api/v1/users/{id}")
    @CircuitBreaker(name = "user-service", fallbackMethod = "getUserByIdFallback")
    ApiResponse<UserResponse> getById(@PathVariable String id);

    default ApiResponse<UserResponse> getUserByIdFallback(Exception e) {
        return ApiResponse.error("User service is not available " + e.getMessage());
    }
}