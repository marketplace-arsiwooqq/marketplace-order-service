package com.arsiwooqq.orderservice.service;

import com.arsiwooqq.orderservice.dto.ItemCreateRequest;
import com.arsiwooqq.orderservice.dto.ItemResponse;
import com.arsiwooqq.orderservice.dto.ItemUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ItemService {
    ItemResponse create(ItemCreateRequest request);

    ItemResponse getById(UUID id);

    ItemResponse update(UUID id, ItemUpdateRequest request);

    void delete(UUID id);

    Page<ItemResponse> getAll(Pageable pageable);
}
