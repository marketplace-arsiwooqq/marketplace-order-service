package com.arsiwooqq.orderservice.mapper;

import com.arsiwooqq.orderservice.dto.OrderItemRequest;
import com.arsiwooqq.orderservice.dto.OrderItemResponse;
import com.arsiwooqq.orderservice.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderItemMapper {
    OrderItemResponse toResponse(OrderItem orderItem);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "item", ignore = true)
    OrderItem toEntity(OrderItemRequest orderItemRequest);
}
