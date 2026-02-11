package com.arsiwooqq.orderservice.security;

import com.arsiwooqq.orderservice.dto.OrderCreateRequest;
import com.arsiwooqq.orderservice.enums.OrderStatus;
import com.arsiwooqq.orderservice.exception.AccessDeniedException;
import com.arsiwooqq.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service("securityService")
@RequiredArgsConstructor
@Slf4j
public class SecurityService {
    private final OrderRepository orderRepository;

    public boolean canCreateOrder(String userId, OrderCreateRequest request) {
        if (userId == null || request == null || !userId.equals(request.userId())) {
            throw new AccessDeniedException("You do not have rights to create this order");
        }

        return true;
    }

    public boolean canAccessOrder(String userId, UUID orderId) {
        if (userId == null || orderId == null) {
            throw new AccessDeniedException("You do not have rights to access this order");
        }

        return orderRepository.findById(orderId)
                .filter(order -> order.getUserId().equals(userId))
                .map(order -> true)
                .orElseThrow(() -> new AccessDeniedException("You do not have rights to access this order"));
    }

    public boolean canManageOrder(String userId, UUID orderId) {
        if (userId == null || orderId == null) {
            throw new AccessDeniedException("You do not have rights to manage this order");
        }

        return orderRepository.findById(orderId)
                .filter(order -> order.getUserId().equals(userId))
                .filter(order -> order.getStatus().equals(OrderStatus.CREATED))
                .map(order -> true)
                .orElseThrow(() -> new AccessDeniedException("You do not have rights to manage this order"));
    }

    public boolean canAccessOrders(String userId, List<UUID> orderIds) {
        if (userId == null || orderIds == null) {
            throw new AccessDeniedException("You do not have rights to access these orders");
        }

        return orderRepository.findByIdIn(orderIds).stream()
                .allMatch(order -> order.getUserId().equals(userId));
    }
}
