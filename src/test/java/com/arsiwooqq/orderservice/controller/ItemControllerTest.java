package com.arsiwooqq.orderservice.controller;

import com.arsiwooqq.orderservice.dto.ItemCreateRequest;
import com.arsiwooqq.orderservice.dto.ItemUpdateRequest;
import com.arsiwooqq.orderservice.entity.Item;
import com.arsiwooqq.orderservice.repository.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
class ItemControllerTest extends AbstractIntegrationTest {

    @Autowired
    private ItemRepository itemRepository;

    @MockitoBean
    protected KafkaAdmin kafkaAdmin;

    @MockitoBean
    protected KafkaListenerEndpointRegistry registry;

    @BeforeEach
    void clearRepositories() {
        itemRepository.deleteAll();
    }

    @Nested
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Create item")
    class CreateItemTests {
        @Test
        @DisplayName("Should create item when valid request provided")
        void givenValidRequest_whenCreate_thenSavesItem() throws Exception {
            var request = new ItemCreateRequest(
                    "TEST_NAME",
                    100L
            );

            mockMvc.perform(post("/api/v1/items")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpectAll(
                            jsonPath("$.data.id").exists(),
                            jsonPath("$.data.name").value(request.name()),
                            jsonPath("$.data.price").value(request.price())
                    );

            var item = itemRepository.findAll().get(0);
            assertEquals(request.name(), item.getName());
            assertEquals(request.price(), item.getPrice());
        }

        @Test
        @DisplayName("Should return bad request when invalid request provided")
        void givenInvalidRequest_whenCreate_thenReturnsBadRequest() throws Exception {
            var request = new ItemCreateRequest(
                    "",
                    -100L
            );

            mockMvc.perform(post("/api/v1/items")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            assertEquals(0, itemRepository.count());
        }
    }

    @Nested
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Get By Id")
    class GetItemTests {
        @Test
        @DisplayName("Should get item when valid id provided")
        void givenValidId_whenGetById_thenReturnsItem() throws Exception {
            var item = createItem();

            mockMvc.perform(get("/api/v1/items/{id}", item.getId()))
                    .andExpect(status().isOk())
                    .andExpectAll(
                            jsonPath("$.data.id").value(item.getId().toString()),
                            jsonPath("$.data.name").value("TEST_NAME"),
                            jsonPath("$.data.price").value(100L)
                    );
        }

        @Test
        @DisplayName("Should return not found when item does not exist")
        void givenNotExistingId_whenGetById_thenReturnsNotFound() throws Exception {
            mockMvc.perform(get("/api/v1/items/{id}", UUID.randomUUID()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return bad request when id not valid")
        void givenInvalidId_whenGetById_thenReturnsBadRequest() throws Exception {
            mockMvc.perform(get("/api/v1/items/{id}", "123"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Get All")
    class GetAllItemsTests {
        @Test
        @DisplayName("Should get all items")
        void givenNoParams_whenGetAll_thenReturnsAllItemsInPage() throws Exception {
            createItem(1);
            createItem(2);

            mockMvc.perform(get("/api/v1/items"))
                    .andExpect(status().isOk())
                    .andExpectAll(
                            jsonPath("$.success").value(true),
                            jsonPath("$.data.page.totalElements").value(2),
                            jsonPath("$.data.page.totalPages").value(1),
                            jsonPath("$.data.content.size()").value(2)
                    );
        }

        @Test
        @DisplayName("Should get all items with pagination")
        void givenPageableParams_whenGetAll_thenReturnsAllItemsWithPagination() throws Exception {
            createItem(1);
            createItem(2);

            mockMvc.perform(get("/api/v1/items")
                            .param("page", "0")
                            .param("size", "1"))
                    .andExpect(status().isOk())
                    .andExpectAll(
                            jsonPath("$.success").value(true),
                            jsonPath("$.data.page.totalElements").value(2),
                            jsonPath("$.data.page.totalPages").value(2),
                            jsonPath("$.data.content.size()").value(1)
                    );
        }
    }

    @Nested
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Update item")
    class UpdateItemTests {
        @Test
        @DisplayName("Should update item when valid request provided")
        void givenValidRequest_whenUpdate_thenUpdatesItem() throws Exception {
            var item = createItem();

            var request = new ItemUpdateRequest(
                    "TEST_NAME2",
                    200L
            );

            mockMvc.perform(patch("/api/v1/items/{id}", item.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpectAll(
                            jsonPath("$.data.id").value(item.getId().toString()),
                            jsonPath("$.data.name").value(request.name()),
                            jsonPath("$.data.price").value(request.price())
                    );

            var updatedItem = itemRepository.findById(item.getId()).orElse(null);
            assertNotNull(updatedItem);
            assertEquals(request.name(), updatedItem.getName());
            assertEquals(request.price(), updatedItem.getPrice());
        }

        @Test
        @DisplayName("Should return bad request when invalid request provided")
        void givenInvalidRequest_whenUpdate_thenReturnsBadRequest() throws Exception {
            var item = createItem();

            var request = new ItemUpdateRequest(
                    "",
                    -100L
            );

            mockMvc.perform(patch("/api/v1/items/{id}", item.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            var updatedItem = itemRepository.findById(item.getId()).orElse(null);
            assertNotNull(updatedItem);
            assertEquals(item.getName(), updatedItem.getName());
            assertEquals(item.getPrice(), updatedItem.getPrice());
        }
    }

    @Nested
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Delete item")
    class DeleteItemTests {
        @Test
        @DisplayName("Should delete item when valid id provided")
        void givenValidId_whenDelete_thenDeletesItem() throws Exception {
            var item = createItem();

            mockMvc.perform(delete("/api/v1/items/{id}", item.getId()))
                    .andExpect(status().isOk());

            var deletedItem = itemRepository.findById(item.getId()).orElse(null);
            assertNull(deletedItem);
        }

        @Test
        @DisplayName("Should return not found when not existing id provided")
        void givenNotExistingId_whenDelete_thenReturnsNotFound() throws Exception {
            mockMvc.perform(delete("/api/v1/items/{id}", UUID.randomUUID()))
                    .andExpect(status().isNotFound());
        }
    }

    private Item createItem() {
        var item = new Item();
        item.setName("TEST_NAME");
        item.setPrice(100L);
        return itemRepository.save(item);
    }

    private Item createItem(int i) {
        var item = new Item();
        item.setName("TEST_NAME" + i);
        item.setPrice(100L * i);
        return itemRepository.save(item);
    }
}
