package com.arsiwooqq.orderservice.service.impl;

import com.arsiwooqq.orderservice.dto.OrderItemRequest;
import com.arsiwooqq.orderservice.entity.OrderItem;
import com.arsiwooqq.orderservice.exception.ItemNotFoundException;
import com.arsiwooqq.orderservice.mapper.OrderItemMapper;
import com.arsiwooqq.orderservice.repository.ItemRepository;
import com.arsiwooqq.orderservice.service.OrderItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderItemServiceImpl implements OrderItemService {
    private final ItemRepository itemRepository;
    private final OrderItemMapper orderItemMapper;

    @Override
    public OrderItem create(OrderItemRequest request) {
        log.debug("Mapping order item for item ID: {}", request.itemId());
        var item = itemRepository.findById(request.itemId())
                .orElseThrow(() -> {
                    log.debug("Item not found with ID: {}", request.itemId());
                    return new ItemNotFoundException(request.itemId());
                });
        var orderItem = orderItemMapper.toEntity(request);
        orderItem.setItem(item);
        log.debug("Successfully mapped order item for item ID: {}", request.itemId());
        return orderItem;
    }
}
