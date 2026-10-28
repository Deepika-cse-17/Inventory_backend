package com.warehouse.inventory.service;

import com.warehouse.inventory.model.InventoryItem;
import com.warehouse.inventory.model.StockMovement;
import com.warehouse.inventory.repository.InventoryItemRepository;
import com.warehouse.inventory.repository.StockMovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InventoryServiceTest {
    private InventoryItemRepository inventoryRepository;
    private StockMovementRepository movementRepository;
    private InventoryService service;

    @BeforeEach
    void setUp() {
        inventoryRepository = mock(InventoryItemRepository.class);
        movementRepository = mock(StockMovementRepository.class);
        service = new InventoryService(inventoryRepository, movementRepository);
    }

    @Test
    void addItemRejectsDuplicateProductId() {
        InventoryItem duplicate = item("SKU-1", 4, 5);
        when(inventoryRepository.existsById("SKU-1")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.addItem(duplicate));
        verify(inventoryRepository, never()).save(any(InventoryItem.class));
    }

    @Test
    void addItemRecordsOpeningQuantityAsStockIn() {
        InventoryItem item = item("SKU-2", 12, 5);
        when(inventoryRepository.existsById("SKU-2")).thenReturn(false);
        when(inventoryRepository.countByCategory("Food")).thenReturn(0L);
        when(inventoryRepository.save(any(InventoryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryRepository.findByProductIdForUpdate("SKU-2")).thenReturn(Optional.of(item));
        when(inventoryRepository.findByProductId("SKU-2")).thenReturn(Optional.of(item));
        when(movementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.addItem(item);

        assertEquals(12, item.getQuantity());
        verify(movementRepository).save(argThat(movement ->
                movement.getType() == StockMovement.MovementType.IN && movement.getQuantity() == 12));
    }

    @Test
    void stockOutCannotExceedAvailableQuantity() {
        InventoryItem item = item("SKU-3", 3, 5);
        when(inventoryRepository.findByProductIdForUpdate("SKU-3")).thenReturn(Optional.of(item));

        assertThrows(IllegalArgumentException.class, () -> service.stockOut("SKU-3", 4, "operator"));
        verify(movementRepository, never()).save(any(StockMovement.class));
    }

    @Test
    void stockOutUpdatesQuantityAndRecordsMovement() {
        InventoryItem item = item("SKU-8", 8, 5);
        when(inventoryRepository.findByProductIdForUpdate("SKU-8")).thenReturn(Optional.of(item));
        when(movementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockMovement movement = service.stockOut("SKU-8", 3, "operator");

        assertEquals(5, item.getQuantity());
        assertEquals(StockMovement.MovementType.OUT, movement.getType());
        assertEquals(3, movement.getQuantity());
        assertEquals("operator", movement.getPerformedBy());
    }

    @Test
    void deletingItemRecordsRemainingQuantityAsStockOut() {
        InventoryItem item = item("SKU-9", 6, 5);
        when(inventoryRepository.findByProductIdForUpdate("SKU-9")).thenReturn(Optional.of(item));
        when(movementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean deleted = service.deleteItem("SKU-9");

        assertTrue(deleted);
        verify(movementRepository).save(argThat(movement ->
                movement.getType() == StockMovement.MovementType.OUT && movement.getQuantity() == 6));
        verify(inventoryRepository).delete(item);
    }

    @Test
    void addItemRejectsNegativeQuantityAndPastExpiration() {
        InventoryItem negativeQuantity = item("SKU-6", -1, 5);
        assertThrows(IllegalArgumentException.class, () -> service.addItem(negativeQuantity));

        InventoryItem expired = item("SKU-7", 1, 5);
        expired.setExpirationDate(LocalDate.now().minusDays(1));
        assertThrows(IllegalArgumentException.class, () -> service.addItem(expired));
        verify(inventoryRepository, never()).save(any(InventoryItem.class));
    }

    @Test
    void lowStockUsesEachItemsReorderLevel() {
        InventoryItem lowStock = item("SKU-4", 5, 8);
        InventoryItem healthyStock = item("SKU-5", 5, 4);
        when(inventoryRepository.findAll()).thenReturn(List.of(lowStock, healthyStock));

        assertEquals(List.of(lowStock), service.getLowStockItems(null));
    }

    private InventoryItem item(String productId, int quantity, int reorderLevel) {
        InventoryItem item = new InventoryItem(
                productId, productId, quantity, LocalDate.now().plusDays(45), "Food", 2.50);
        item.setReorderLevel(reorderLevel);
        return item;
    }
}
