package com.arsiwooqq.orderservice.service.impl;

import com.arsiwooqq.orderservice.dto.ChangeOrderStatusRequest;
import com.arsiwooqq.orderservice.dto.OrderCreateRequest;
import com.arsiwooqq.orderservice.dto.OrderResponse;
import com.arsiwooqq.orderservice.dto.OrderUpdateRequest;
import com.arsiwooqq.orderservice.entity.Order;
import com.arsiwooqq.orderservice.enums.OrderStatus;
import com.arsiwooqq.orderservice.event.publisher.OrderEventPublisher;
import com.arsiwooqq.orderservice.exception.OrderNotFoundException;
import com.arsiwooqq.orderservice.mapper.OrderMapper;
import com.arsiwooqq.orderservice.repository.OrderRepository;
import com.arsiwooqq.orderservice.service.OrderItemService;
import com.arsiwooqq.orderservice.service.OrderService;
import com.arsiwooqq.orderservice.service.UserDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderItemService orderItemService;
    private final UserDataService userDataService;
    private final OrderEventPublisher orderEventPublisher;

    /**
     * Creates a new order. Sets the creation date to the current date and status to "CREATED".
     *
     * @param request The order creation request.
     * @return The created order response.
     */
    @Override
    public OrderResponse create(OrderCreateRequest request) {
        log.debug("Creating new order for user: {}", request.userId());
        var order = orderMapper.toEntity(request);
        var orderItems = request.orderItems().stream()
                .map(orderItemService::create)
                .peek(orderItem -> orderItem.setOrder(order))
                .toList();
        order.setOrderItems(orderItems);
        order.setStatus(OrderStatus.CREATED);
        order.setCreationDate(LocalDate.now());

        log.trace("Saving order to database");
        var savedOrder = orderRepository.save(order);
        log.debug("Order created successfully with ID: {}", savedOrder.getId());

        var event = orderMapper.toOrderCreatedEvent(order, getOrderAmount(order));
        orderEventPublisher.publishOrderCreated(event);

        return orderMapper.toResponse(savedOrder, userDataService.fetchUserData(order.getUserId()));
    }

    /**
     * Retrieves an order by its ID.
     *
     * @param id The order ID.
     * @return The order response.
     * @throws OrderNotFoundException If no orders are found for the given IDs.
     */
    @Override
    @Transactional(readOnly = true)
    public OrderResponse getById(UUID id) {
        log.debug("Fetching order by ID: {}", id);
        return orderRepository
                .findById(id)
                .map(order -> {
                    log.trace("Order found, fetching user data for user ID: {}", order.getUserId());
                    var response = orderMapper.toResponse(order, userDataService.fetchUserData(order.getUserId()));
                    log.debug("Successfully fetched order with ID: {}", id);
                    return response;
                })
                .orElseThrow(() -> {
                    log.debug("Order not found with ID: {}", id);
                    return new OrderNotFoundException(id);
                });
    }

    /**
     * Retrieves multiple orders by their IDs.
     *
     * @param ids The list of order IDs.
     * @return The list of order responses.
     */
    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllByIds(List<UUID> ids) {
        log.debug("Fetching orders by IDs, count: {}", ids.size());
        var orders = orderRepository
                .findByIdIn(ids)
                .stream()
                .map(order -> {
                    log.trace("Processing order with ID: {} for user ID: {}", order.getId(), order.getUserId());
                    return orderMapper.toResponse(order, userDataService.fetchUserData(order.getUserId()));
                })
                .toList();
        log.debug("Successfully fetched {} orders by IDs", orders.size());
        return orders;
    }

    /**
     * Retrieves multiple orders by their statuses.
     *
     * @param statuses The list of order statuses.
     * @return The list of order responses.
     */
    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllByStatuses(List<String> statuses) {
        log.debug("Fetching orders by statuses: {}", statuses);

        var orderStatuses = statuses.stream()
                .map(s -> {
                    try {
                        return OrderStatus.fromString(s);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();

        var orders = orderRepository
                .findByStatusIn(orderStatuses)
                .stream()
                .map(order -> {
                    log.trace("Processing order with ID: {} and status: {}", order.getId(), order.getStatus());
                    return orderMapper.toResponse(order, userDataService.fetchUserData(order.getUserId()));
                })
                .toList();
        log.debug("Successfully fetched {} orders by statuses", orders.size());
        return orders;
    }

    /**
     * Updates an existing order. Deletes existing OrderItems and creates new ones.
     *
     * @param id The order ID.
     * @param request The order update request.
     * @return The updated order response.
     * @throws OrderNotFoundException If no orders are found for the given ID.
     */
    @Override
    @Transactional
    public OrderResponse update(UUID id, OrderUpdateRequest request) {
        log.debug("Updating order with ID: {}", id);
        var order = orderRepository
                .findById(id)
                .orElseThrow(() -> {
                    log.debug("Order for update not found with ID: {}", id);
                    return new OrderNotFoundException(id);
                });

        log.debug("Mapping order items for order ID: {}", id);
        var orderItems = request.orderItems().stream()
                .map(orderItemService::create)
                .peek(orderItem -> orderItem.setOrder(order))
                .toList();

        order.getOrderItems().clear();
        orderItems.forEach(order::addOrderItem);

        log.trace("Saving updated order to database");
        var savedOrder = orderRepository.save(order);
        log.debug("Order updated successfully for ID: {}", savedOrder.getId());

        return orderMapper.toResponse(savedOrder, userDataService.fetchUserData(savedOrder.getUserId()));
    }

    /**
     * Changes the status of an existing order.
     *
     * @param id      The order ID.
     * @param request The change order status request.
     * @return The updated order response.
     */
    @Override
    @Transactional
    public OrderResponse changeStatus(UUID id, ChangeOrderStatusRequest request) {
        log.debug("Changing status for order with ID: {}", id);
        var order = orderRepository
                .findById(id)
                .orElseThrow(() -> {
                    log.debug("Order for changing status not found with ID: {}", id);
                    return new OrderNotFoundException(id);
                });
        order.setStatus(request.status());
        log.trace("Saving order with new status to database");
        orderRepository.save(order);
        log.debug("Order status changed successfully for ID: {}", order.getId());

        return orderMapper.toResponse(order, userDataService.fetchUserData(order.getUserId()));
    }

    /**
     * Deletes an existing order.
     *
     * @param id The order ID.
     * @throws OrderNotFoundException If no orders are found for the given ID.
     */
    @Override
    @Transactional
    public void delete(UUID id) {
        log.debug("Deleting order with ID: {}", id);
        var order = orderRepository
                .findById(id)
                .orElseThrow(() -> {
                    log.debug("Order for deletion not found with ID: {}", id);
                    return new OrderNotFoundException(id);
                });
        orderRepository.delete(order);
        log.debug("Order deleted successfully with ID: {}", id);
    }

    private Long getOrderAmount(Order order) {
        return order.getOrderItems().stream()
                .mapToLong(orderItem -> orderItem.getItem().getPrice() * orderItem.getQuantity())
                .sum();
    }
}