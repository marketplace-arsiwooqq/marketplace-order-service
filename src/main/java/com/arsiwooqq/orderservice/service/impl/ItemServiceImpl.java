package com.arsiwooqq.orderservice.service.impl;

import com.arsiwooqq.orderservice.dto.ItemCreateRequest;
import com.arsiwooqq.orderservice.dto.ItemResponse;
import com.arsiwooqq.orderservice.dto.ItemUpdateRequest;
import com.arsiwooqq.orderservice.exception.ItemNotFoundException;
import com.arsiwooqq.orderservice.mapper.ItemMapper;
import com.arsiwooqq.orderservice.repository.ItemRepository;
import com.arsiwooqq.orderservice.service.ItemService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    @Override
    public ItemResponse create(ItemCreateRequest request) {
        log.debug("Creating new item with name: {}", request.name());
        var savedItem = itemRepository.save(itemMapper.toEntity(request));
        log.debug("Item created successfully with ID: {}", savedItem.getId());
        return itemMapper.toResponse(savedItem);
    }

    @Override
    public ItemResponse getById(UUID id) {
        log.debug("Fetching item by ID: {}", id);
        return itemRepository
                .findById(id)
                .map(item -> {
                    log.debug("Item found with ID: {}", id);
                    return itemMapper.toResponse(item);
                })
                .orElseThrow(() -> {
                    log.debug("Item not found with ID: {}", id);
                    return new ItemNotFoundException(id);
                });
    }

    @Override
    public Page<ItemResponse> getAll(Pageable pageable) {
        log.debug("Fetching page of items with page number: {} and page size: {}",
                pageable.getPageNumber(), pageable.getPageSize());
        var page = itemRepository.findAll(pageable)
                .map(itemMapper::toResponse);
        log.debug("Successfully fetched page of items, total elements: {}", page.getTotalElements());
        return page;
    }

    @Override
    @Transactional
    public ItemResponse update(UUID id, ItemUpdateRequest request) {
        log.debug("Updating item with ID: {}", id);
        var item = itemRepository
                .findById(id)
                .orElseThrow(() -> {
                    log.debug("Updating item not found. ID: {}", id);
                    return new ItemNotFoundException(id);
                });
        itemMapper.update(request, item);
        var savedItem = itemRepository.save(item);
        log.debug("Item updated successfully with ID: {}", savedItem.getId());
        return itemMapper.toResponse(savedItem);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        log.debug("Deleting item with ID: {}", id);
        var item = itemRepository
                .findById(id)
                .orElseThrow(() -> {
                    log.debug("Item for deletion not found with ID: {}", id);
                    return new ItemNotFoundException(id);
                });
        itemRepository.delete(item);
        log.debug("Item deleted successfully with ID: {}", id);
    }
}