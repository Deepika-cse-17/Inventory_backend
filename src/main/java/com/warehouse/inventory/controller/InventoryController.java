package com.warehouse.inventory.controller;

import com.warehouse.inventory.model.InventoryItem;
import com.warehouse.inventory.service.InventoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for inventory management operations.
 */
@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "*")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /**
     * Add a new item to inventory or update existing item quantity.
     * POST /api/inventory/add
     */
    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addItem(@RequestBody InventoryItem item) {
        try {
            boolean success = inventoryService.addItem(item);
            if (success) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Item added successfully",
                    "item", item
                ));
            } else {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "success", false,
                    "message", "Failed to add item: Category capacity exceeded"
                ));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Remove an item from inventory.
     * DELETE /api/inventory/remove/{productId}?quantity={quantity}
     */
    @DeleteMapping("/remove/{productId}")
    public ResponseEntity<Map<String, Object>> removeItem(
            @PathVariable String productId,
            @RequestParam(required = false) Integer quantity) {
        try {
            boolean success = inventoryService.removeItem(productId, quantity);
            if (success) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", quantity != null && quantity > 0 
                        ? "Quantity reduced successfully" 
                        : "Item removed successfully"
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", "Item not found"
                ));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get all items sorted by quantity.
     * GET /api/inventory/sorted/quantity
     */
    @GetMapping("/sorted/quantity")
    public ResponseEntity<Map<String, Object>> getItemsSortedByQuantity() {
        List<InventoryItem> items = inventoryService.getAllItemsSortedByQuantity();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "items", items,
            "count", items.size(),
            "sortBy", "quantity"
        ));
    }

    /**
     * Get all items sorted by expiration date.
     * GET /api/inventory/sorted/expiration
     */
    @GetMapping("/sorted/expiration")
    public ResponseEntity<Map<String, Object>> getItemsSortedByExpirationDate() {
        List<InventoryItem> items = inventoryService.getAllItemsSortedByExpirationDate();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "items", items,
            "count", items.size(),
            "sortBy", "expirationDate"
        ));
    }

    /**
     * Get all items.
     * GET /api/inventory/all
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllItems() {
        List<InventoryItem> items = inventoryService.getAllItems();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "items", items,
            "count", items.size()
        ));
    }

    /**
     * Get items by category.
     * GET /api/inventory/category/{category}
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<Map<String, Object>> getItemsByCategory(@PathVariable String category) {
        List<InventoryItem> items = inventoryService.getItemsByCategory(category);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "items", items,
            "count", items.size(),
            "category", category
        ));
    }

    /**
     * Get a specific item by productId.
     * GET /api/inventory/{productId}
     */
    @GetMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> getItemById(@PathVariable String productId) {
        Optional<InventoryItem> item = inventoryService.getItemById(productId);
        if (item.isPresent()) {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "item", item.get()
            ));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "success", false,
                "message", "Item not found"
            ));
        }
    }

    /**
     * Get inventory as 2D array representation.
     * GET /api/inventory/2d-array
     */
    @GetMapping("/2d-array")
    public ResponseEntity<Map<String, Object>> getInventoryAs2DArray() {
        Map<String, InventoryItem[][]> inventory2D = inventoryService.getInventoryAs2DArray();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "inventory2D", inventory2D,
            "structure", "Each category contains a 2D array [1][n] where n is the number of items"
        ));
    }

    /**
     * Get items with low stock.
     * GET /api/inventory/low-stock?threshold={threshold}
     */
    @GetMapping("/low-stock")
    public ResponseEntity<Map<String, Object>> getLowStockItems(
            @RequestParam(required = false) Integer threshold) {
        List<InventoryItem> items = inventoryService.getLowStockItems(threshold);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "items", items,
            "count", items.size(),
            "threshold", threshold != null ? threshold : 10
        ));
    }

    /**
     * Get inventory statistics.
     * GET /api/inventory/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = inventoryService.getInventoryStatistics();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "statistics", stats
        ));
    }
}

