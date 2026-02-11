package com.arsiwooqq.orderservice.mapper;

import com.arsiwooqq.orderservice.dto.OrderCreateRequest;
import com.arsiwooqq.orderservice.dto.OrderResponse;
import com.arsiwooqq.orderservice.dto.UserResponse;
import com.arsiwooqq.orderservice.entity.Order;
import com.arsiwooqq.orderservice.event.OrderCreatedEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {OrderItemMapper.class})
public interface OrderMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "creationDate", ignore = true)
    Order toEntity(OrderCreateRequest request);

    @Mapping(target = "id", source = "order.id")
    @Mapping(target = "userResponse", source = "user")
    @Mapping(target = "userId", source = "order.userId")
    OrderResponse toResponse(Order order, UserResponse user);

    default OrderCreatedEvent toOrderCreatedEvent(Order order, Long paymentAmount) {
        return new OrderCreatedEvent(
                order.getId(),
                order.getUserId(),
                paymentAmount
        );
    }
}
