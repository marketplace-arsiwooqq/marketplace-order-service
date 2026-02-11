package com.arsiwooqq.orderservice.repository;

import com.arsiwooqq.orderservice.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ItemRepository extends JpaRepository<Item, UUID> {
}
