package com.arsiwooqq.orderservice.dto;

import com.arsiwooqq.orderservice.entity.Item;

public record OrderItemResponse(
        Item item,
        Integer quantity
) {
}
