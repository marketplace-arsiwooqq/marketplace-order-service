package com.arsiwooqq.orderservice.service;

import com.arsiwooqq.orderservice.dto.OrderItemRequest;
import com.arsiwooqq.orderservice.entity.OrderItem;

public interface OrderItemService {
    OrderItem create(OrderItemRequest request);
}
