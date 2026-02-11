package com.arsiwooqq.orderservice.service.impl;

import com.arsiwooqq.orderservice.dto.ItemCreateRequest;
import com.arsiwooqq.orderservice.dto.ItemResponse;
import com.arsiwooqq.orderservice.dto.ItemUpdateRequest;
import com.arsiwooqq.orderservice.entity.Item;
import com.arsiwooqq.orderservice.exception.ItemNotFoundException;
import com.arsiwooqq.orderservice.mapper.ItemMapper;
import com.arsiwooqq.orderservice.repository.ItemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

    @Mock
    private ItemMapper itemMapper;

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemServiceImpl itemService;

    @Nested
    @DisplayName("Create item")
    class CreateItemTests {
        @Test
        @DisplayName("Should create item")
        void givenItem_whenCreate_thenCreatesItem() {
            // Given
            var request = new ItemCreateRequest("TEST_ITEM", 100L);
            var item = new Item(UUID.randomUUID(), request.name(), request.price());
            var response = new ItemResponse(item.getId(), item.getName(), item.getPrice());

            // When
            when(itemMapper.toEntity(request)).thenReturn(item);
            when(itemRepository.save(item)).thenReturn(item);
            when(itemMapper.toResponse(item)).thenReturn(response);

            var result = itemService.create(request);

            // Then
            assertEquals(response, result);
            verify(itemMapper, times(1)).toEntity(request);
            verify(itemMapper, times(1)).toResponse(item);
            verify(itemRepository, times(1)).save(item);
        }
    }

    @Nested
    @DisplayName("Get item by id")
    class GetItemByIdTests {
        @Test
        @DisplayName("Should return item response")
        void givenId_whenGetById_thenReturnsItemResponse() {
            // Given
            var item = new Item(UUID.randomUUID(), "TEST_NAME", 100L);
            var response = new ItemResponse(item.getId(), item.getName(), item.getPrice());

            // When
            when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
            when(itemMapper.toResponse(item)).thenReturn(response);

            var result = itemService.getById(item.getId());

            // Then
            assertEquals(response, result);
            verify(itemRepository, times(1)).findById(item.getId());
            verify(itemMapper, times(1)).toResponse(item);
        }

        @Test
        @DisplayName("Should throw ItemNotFoundException when item not found")
        void givenNotExistingId_whenGetById_thenThrowsException() {
            // Given
            var id = UUID.randomUUID();

            when(itemRepository.findById(id)).thenReturn(Optional.empty());

            // When, Then
            assertThrows(ItemNotFoundException.class, () -> itemService.getById(id));
        }
    }

    @Nested
    @DisplayName("Get all items")
    class GetAllTests {
        @Test
        @DisplayName("Should return page of items")
        void givenItems_whenGetAll_thenReturnsPageOfItems() {
            // Given
            var item = new Item(UUID.randomUUID(), "TEST_NAME", 100L);
            var response = new ItemResponse(item.getId(), item.getName(), item.getPrice());
            var items = List.of(item);
            var pageable = PageRequest.of(0, 10);
            var page = new PageImpl<>(items, pageable, items.size());

            // When
            when(itemRepository.findAll(pageable)).thenReturn(page);
            when(itemMapper.toResponse(item)).thenReturn(response);

            var result = itemService.getAll(pageable);

            // Then
            assertEquals(page.getTotalPages(), result.getTotalPages());
            assertEquals(page.getTotalElements(), result.getTotalElements());
            assertEquals(response, result.getContent().get(0));

            verify(itemRepository, times(1)).findAll(pageable);
            verifyNoMoreInteractions(itemRepository);
            verify(itemMapper, times(items.size())).toResponse(item);
            verifyNoMoreInteractions(itemMapper);
        }

        @Test
        @DisplayName("Should return empty page")
        void givenNoItems_whenGetAll_thenReturnsEmptyPage() {
            // Given
            var pageable = PageRequest.of(0, 10);
            var page = new PageImpl<Item>(List.of(), pageable, 0);

            // When
            when(itemRepository.findAll(pageable)).thenReturn(page);

            var result = itemService.getAll(pageable);

            // Then
            assertEquals(page.getTotalPages(), result.getTotalPages());
            assertEquals(page.getTotalElements(), result.getTotalElements());

            verify(itemRepository, times(1)).findAll(pageable);
            verifyNoMoreInteractions(itemRepository);
            verify(itemMapper, never()).toResponse(any());
        }
    }

    @Nested
    @DisplayName("Update item")
    class UpdateItemTests {
        @Test
        @DisplayName("Should update item")
        void givenItem_whenUpdate_thenReturnsUpdatedItem() {
            // Given
            var request = new ItemUpdateRequest("TEST_NAME2", 200L);
            var item = new Item(UUID.randomUUID(), "TEST_NAME", 100L);
            var response = new ItemResponse(item.getId(), request.name(), request.price());

            // When
            when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
            when(itemRepository.save(item)).thenReturn(item);
            when(itemMapper.toResponse(item)).thenReturn(response);
            doAnswer(invocation -> {
                Item updatingItem = invocation.getArgument(1);
                updatingItem.setName(request.name());
                updatingItem.setPrice(response.price());
                return null;
            }).when(itemMapper).update(request, item);

            var result = itemService.update(item.getId(), request);

            // Then
            assertEquals(response, result);
            verify(itemRepository, times(1)).findById(item.getId());
            verify(itemMapper, times(1)).update(request, item);
            verify(itemRepository, times(1)).save(item);
            verify(itemMapper, times(1)).toResponse(item);
        }

        @Test
        @DisplayName("Should update item")
        void givenNotExistingItem_whenUpdate_thenThrowsException() {
            // Given
            var id = UUID.randomUUID();
            var request = new ItemUpdateRequest("TEST_NAME2", 200L);

            // When
            when(itemRepository.findById(id)).thenReturn(Optional.empty());

            // Then
            assertThrows(ItemNotFoundException.class, () -> itemService.update(id, request));

            verify(itemRepository, times(1)).findById(id);
            verify(itemMapper, never()).update(any(), any());
            verify(itemRepository, never()).save(any());
            verify(itemMapper, never()).toResponse(any());
        }
    }

    @Nested
    @DisplayName("Delete item")
    class DeleteItemTests {
        @Test
        @DisplayName("Should delete item")
        void givenId_whenDelete_thenDeletesItem() {
            // Given
            var item = new Item(UUID.randomUUID(), "TEST_NAME", 100L);

            // When
            when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));

            itemService.delete(item.getId());

            // Then
            verify(itemRepository, times(1)).findById(item.getId());
            verify(itemRepository, times(1)).delete(item);
        }

        @Test
        @DisplayName("Should throw ItemNotFoundException when item not found")
        void givenId_whenDelete_thenThrowsItemNotFoundException() {
            // Given
            var id = UUID.randomUUID();

            // When
            when(itemRepository.findById(id)).thenReturn(Optional.empty());

            // Then
            assertThrows(ItemNotFoundException.class, () -> itemService.delete(id));

            verify(itemRepository, times(1)).findById(id);
            verify(itemRepository, never()).delete(any());
        }
    }
}
