package com.arsiwooqq.orderservice.service;

import com.arsiwooqq.orderservice.dto.ChangeOrderStatusRequest;
import com.arsiwooqq.orderservice.dto.OrderCreateRequest;
import com.arsiwooqq.orderservice.dto.OrderResponse;
import com.arsiwooqq.orderservice.dto.OrderUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface OrderService {
    OrderResponse create(OrderCreateRequest request);

    OrderResponse getById(UUID id);

    List<OrderResponse> getAllByIds(List<UUID> ids);

    List<OrderResponse> getAllByStatuses(List<String> statuses);

    OrderResponse update(UUID id, OrderUpdateRequest request);

    OrderResponse changeStatus(UUID id, ChangeOrderStatusRequest request);

    void delete(UUID id);
}
