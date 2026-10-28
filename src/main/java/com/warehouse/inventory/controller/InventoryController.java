package com.warehouse.inventory.controller;

import com.warehouse.inventory.dto.StockMovementRequest;
import com.warehouse.inventory.model.InventoryItem;
import com.warehouse.inventory.model.StockMovement;
import com.warehouse.inventory.service.InventoryService;
import com.warehouse.inventory.service.InventoryService.ItemNotFoundException;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "name", "productName",
            "productId", "productId",
            "quantity", "quantity",
            "expirationDate", "expirationDate",
            "unitPrice", "unitPrice",
            "category", "category");

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addItem(@Valid @RequestBody InventoryItem item) {
        try {
            InventoryItem created = inventoryService.addItem(item);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Item added successfully",
                    "item", created));
        } catch (IllegalStateException | DataIntegrityViolationException exception) {
            return error(HttpStatus.CONFLICT, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return error(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }

    @PutMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> updateItem(
            @PathVariable String productId,
            @Valid @RequestBody InventoryItem changes) {
        try {
            InventoryItem updated = inventoryService.updateItem(productId, changes);
            return ResponseEntity.ok(Map.of("success", true, "message", "Item updated successfully", "item", updated));
        } catch (ItemNotFoundException exception) {
            return error(HttpStatus.NOT_FOUND, exception.getMessage());
        } catch (IllegalStateException exception) {
            return error(HttpStatus.CONFLICT, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return error(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> deleteItem(@PathVariable String productId) {
        if (!inventoryService.deleteItem(productId)) {
            return error(HttpStatus.NOT_FOUND, "Item not found");
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Item deleted successfully"));
    }

    @PostMapping("/stock-in")
    public ResponseEntity<Map<String, Object>> stockIn(
            @Valid @RequestBody StockMovementRequest request,
            Authentication authentication) {
        try {
            StockMovement movement = inventoryService.stockIn(
                    request.getProductId(), request.getQuantity(), authentication.getName());
            InventoryItem item = inventoryService.getItemById(request.getProductId()).orElseThrow();
            return ResponseEntity.ok(Map.of(
                    "success", true, "message", "Stock received successfully", "item", item, "movement", movement));
        } catch (ItemNotFoundException exception) {
            return error(HttpStatus.NOT_FOUND, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return error(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }

    @PostMapping("/stock-out")
    public ResponseEntity<Map<String, Object>> stockOut(
            @Valid @RequestBody StockMovementRequest request,
            Authentication authentication) {
        try {
            StockMovement movement = inventoryService.stockOut(
                    request.getProductId(), request.getQuantity(), authentication.getName());
            InventoryItem item = inventoryService.getItemById(request.getProductId()).orElseThrow();
            return ResponseEntity.ok(Map.of(
                    "success", true, "message", "Stock issued successfully", "item", item, "movement", movement));
        } catch (ItemNotFoundException exception) {
            return error(HttpStatus.NOT_FOUND, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return error(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }

    @GetMapping("/{productId}/movements")
    public ResponseEntity<Map<String, Object>> getMovements(@PathVariable String productId) {
        if (inventoryService.getItemById(productId).isEmpty()) {
            return error(HttpStatus.NOT_FOUND, "Item not found");
        }
        List<StockMovement> movements = inventoryService.getMovements(productId);
        return ResponseEntity.ok(Map.of("success", true, "movements", movements, "count", movements.size()));
    }

    @GetMapping("/movements")
    public ResponseEntity<Map<String, Object>> getAllMovements(
            @RequestParam(required = false) String productId) {
        List<StockMovement> movements = productId == null || productId.isBlank()
                ? inventoryService.getAllMovements()
                : inventoryService.getMovements(productId);
        return ResponseEntity.ok(Map.of("success", true, "movements", movements, "count", movements.size()));
    }

    /**
     * Legacy route retained for existing clients. A request without a quantity still removes the item.
     */
    @DeleteMapping("/remove/{productId}")
    public ResponseEntity<Map<String, Object>> removeItem(
            @PathVariable String productId,
            @RequestParam(required = false) Integer quantity) {
        try {
            if (inventoryService.removeItem(productId, quantity)) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", quantity != null && quantity > 0
                                ? "Quantity reduced successfully"
                                : "Item removed successfully"));
            }
            return error(HttpStatus.NOT_FOUND, "Item not found");
        } catch (IllegalArgumentException exception) {
            return error(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }

    @GetMapping("/sorted/quantity")
    public ResponseEntity<Map<String, Object>> getItemsSortedByQuantity() {
        List<InventoryItem> items = inventoryService.getAllItemsSortedByQuantity();
        return ResponseEntity.ok(Map.of("success", true, "items", items, "count", items.size(), "sortBy", "quantity"));
    }

    @GetMapping("/sorted/expiration")
    public ResponseEntity<Map<String, Object>> getItemsSortedByExpirationDate() {
        List<InventoryItem> items = inventoryService.getAllItemsSortedByExpirationDate();
        return ResponseEntity.ok(Map.of(
                "success", true, "items", items, "count", items.size(), "sortBy", "expirationDate"));
    }

    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllItems(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false, defaultValue = "false") boolean lowStockOnly,
            @RequestParam(defaultValue = "asc") String direction) {
        boolean paginated = page != null || size != null || search != null || category != null || sort != null || lowStockOnly;
        if (!paginated) {
            List<InventoryItem> items = inventoryService.getAllItems();
            return ResponseEntity.ok(Map.of("success", true, "items", items, "count", items.size()));
        }
        return getPage(search, category, lowStockOnly, page, size, sort, direction);
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchItems(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "false") boolean lowStockOnly,
            @RequestParam(defaultValue = "asc") String direction) {
        return getPage(q, category, lowStockOnly, page, size, sort, direction);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<Map<String, Object>> getItemsByCategory(@PathVariable String category) {
        List<InventoryItem> items = inventoryService.getItemsByCategory(category);
        return ResponseEntity.ok(Map.of("success", true, "items", items, "count", items.size(), "category", category));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> getItemById(@PathVariable String productId) {
        Optional<InventoryItem> item = inventoryService.getItemById(productId);
        return item.map(value -> ResponseEntity.ok(Map.of("success", true, "item", value)))
                .orElseGet(() -> error(HttpStatus.NOT_FOUND, "Item not found"));
    }

    @GetMapping("/2d-array")
    public ResponseEntity<Map<String, Object>> getInventoryAs2DArray() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "inventory2D", inventoryService.getInventoryAs2DArray(),
                "structure", "Each category contains a 2D array [1][n] where n is the number of items"));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<Map<String, Object>> getLowStockItems(
            @RequestParam(required = false) Integer threshold) {
        List<InventoryItem> items = inventoryService.getLowStockItems(threshold);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "items", items,
                "count", items.size(),
                "threshold", threshold != null && threshold > 0 ? threshold : 10,
                "thresholdMode", threshold != null && threshold > 0 ? "global" : "per-product"));
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(Map.of("success", true, "statistics", inventoryService.getInventoryStatistics()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .findFirst()
                .orElse("Invalid request");
        return error(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseEntity<Map<String, Object>> getPage(
            String search, String category, boolean lowStockOnly, Integer page, Integer size, String sort, String direction) {
        int pageNumber = page == null ? 0 : page;
        int pageSize = size == null ? DEFAULT_PAGE_SIZE : size;
        if (pageNumber < 0) return error(HttpStatus.BAD_REQUEST, "Page must be zero or greater");
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            return error(HttpStatus.BAD_REQUEST, "Page size must be between 1 and " + MAX_PAGE_SIZE);
        }
        String sortProperty = SORT_FIELDS.get(sort == null ? "name" : sort);
        if (sortProperty == null) {
            return error(HttpStatus.BAD_REQUEST, "Unsupported sort field");
        }
        Sort.Direction sortDirection;
        try {
            sortDirection = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException exception) {
            return error(HttpStatus.BAD_REQUEST, "Sort direction must be 'asc' or 'desc'");
        }
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(sortDirection, sortProperty));
        Page<InventoryItem> result = inventoryService.searchItems(search, category, lowStockOnly, pageable);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "items", result.getContent(),
                "count", result.getNumberOfElements(),
                "totalItems", result.getTotalElements(),
                "totalPages", result.getTotalPages(),
                "page", result.getNumber(),
                "size", result.getSize()));
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("success", false, "message", message));
    }
}
