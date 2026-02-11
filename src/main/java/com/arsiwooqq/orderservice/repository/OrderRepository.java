package com.arsiwooqq.orderservice.repository;

import com.arsiwooqq.orderservice.entity.Order;
import com.arsiwooqq.orderservice.enums.OrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    @EntityGraph(
            attributePaths = {
                    "orderItems",
                    "orderItems.item"
            }
    )
    List<Order> findByIdIn(List<UUID> ids);

    @EntityGraph(
            attributePaths = {
                    "orderItems",
                    "orderItems.item"
            }
    )
    List<Order> findByStatusIn(List<OrderStatus> statuses);
}
