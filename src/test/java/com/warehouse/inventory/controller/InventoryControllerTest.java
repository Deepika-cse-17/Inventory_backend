package com.warehouse.inventory.controller;

import com.warehouse.inventory.model.InventoryItem;
import com.warehouse.inventory.model.StockMovement;
import com.warehouse.inventory.config.SecurityConfig;
import com.warehouse.inventory.config.WebConfig;
import com.warehouse.inventory.repository.UserRepository;
import com.warehouse.inventory.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryController.class)
@Import({SecurityConfig.class, WebConfig.class})
@WithMockUser(roles = "ADMIN")
class InventoryControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InventoryService inventoryService;

        @MockBean
        private UserRepository userRepository;

    @Test
    void createsInventoryItem() throws Exception {
        InventoryItem item = item("SKU-101", "Green Tea", 12);
        when(inventoryService.addItem(any(InventoryItem.class))).thenReturn(item);

        mockMvc.perform(post("/api/inventory/add").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "SKU-101",
                                  "name": "Green Tea",
                                  "quantity": 12,
                                  "expirationDate": "%s",
                                  "category": "Beverages",
                                  "unitPrice": 4.5,
                                  "reorderLevel": 3
                                }
                                """.formatted(item.getExpirationDate())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.item.productId").value("SKU-101"))
                .andExpect(jsonPath("$.item.quantity").value(12));
    }

    @Test
    void updatesInventoryItemDetails() throws Exception {
        InventoryItem updated = item("SKU-102", "Black Tea", 8);
        when(inventoryService.updateItem(eq("SKU-102"), any(InventoryItem.class))).thenReturn(updated);

        mockMvc.perform(put("/api/inventory/SKU-102").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "SKU-102",
                                  "name": "Black Tea",
                                  "quantity": 0,
                                  "expirationDate": "%s",
                                  "category": "Beverages",
                                  "unitPrice": 5.25,
                                  "reorderLevel": 4
                                }
                                """.formatted(updated.getExpirationDate())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.item.name").value("Black Tea"));
    }

    @Test
    void deletesInventoryItem() throws Exception {
        when(inventoryService.deleteItem("SKU-103")).thenReturn(true);

        mockMvc.perform(delete("/api/inventory/SKU-103").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(inventoryService).deleteItem("SKU-103");
    }

    @Test
    @WithMockUser(roles = "USER")
    void userCannotDeleteInventoryItem() throws Exception {
        mockMvc.perform(delete("/api/inventory/SKU-103").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    void anonymousCannotReadInventory() throws Exception {
        mockMvc.perform(get("/api/inventory/statistics"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void searchesInventoryItems() throws Exception {
        InventoryItem item = item("SKU-104", "Oolong Tea", 7);
        when(inventoryService.searchItems(
                eq("oolong"), isNull(), eq(false), any()))
                .thenReturn(new PageImpl<>(
                        List.of(item),
                        PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "productName")),
                        1));

        mockMvc.perform(get("/api/inventory/search")
                        .param("q", "oolong")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.items[0].productId").value("SKU-104"))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void returnsValidationErrorForInvalidCreateRequest() throws Exception {
        mockMvc.perform(post("/api/inventory/add").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "",
                                  "name": "Green Tea",
                                  "quantity": 2,
                                  "category": "Beverages",
                                  "unitPrice": 4.5,
                                  "reorderLevel": 3
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "inventory-operator", roles = "ADMIN")
    void recordsStockIn() throws Exception {
        StockMovement movement = new StockMovement(
                "SKU-105", StockMovement.MovementType.IN, 5, "inventory-operator");
        when(inventoryService.stockIn("SKU-105", 5, "inventory-operator")).thenReturn(movement);
        when(inventoryService.getItemById("SKU-105")).thenReturn(Optional.of(item("SKU-105", "White Tea", 15)));

        mockMvc.perform(post("/api/inventory/stock-in").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"SKU-105","quantity":5,"user":"forged-operator"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.movement.type").value("IN"))
                .andExpect(jsonPath("$.movement.quantity").value(5))
                .andExpect(jsonPath("$.movement.user").value("inventory-operator"))
                .andExpect(jsonPath("$.item.productId").value("SKU-105"));
        verify(inventoryService).stockIn("SKU-105", 5, "inventory-operator");
    }

    @Test
    @WithMockUser(username = "inventory-operator", roles = "ADMIN")
    void rejectsStockOutWhenQuantityExceedsAvailableStock() throws Exception {
        when(inventoryService.stockOut("SKU-106", 9, "inventory-operator"))
                .thenThrow(new IllegalArgumentException("Stock out quantity exceeds available quantity"));

        mockMvc.perform(post("/api/inventory/stock-out").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"SKU-106","quantity":9,"user":"tester"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Stock out quantity exceeds available quantity"));
        verify(inventoryService).stockOut("SKU-106", 9, "inventory-operator");
    }

    private InventoryItem item(String productId, String name, int quantity) {
        InventoryItem item = new InventoryItem(
                productId, name, quantity, LocalDate.now().plusDays(45), "Beverages", 4.5);
        item.setReorderLevel(3);
        return item;
    }
}
