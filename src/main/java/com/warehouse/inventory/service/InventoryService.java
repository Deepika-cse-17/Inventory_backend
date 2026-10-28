package com.warehouse.inventory.service;

import com.warehouse.inventory.model.InventoryItem;
import com.warehouse.inventory.model.StockMovement;
import com.warehouse.inventory.model.StockMovement.MovementType;
import com.warehouse.inventory.repository.InventoryItemRepository;
import com.warehouse.inventory.repository.StockMovementRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class InventoryService {
    private static final int MAX_CAPACITY_PER_CATEGORY = 1000;
    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 10;

    private final InventoryItemRepository inventoryRepository;
    private final StockMovementRepository movementRepository;

    public InventoryService(
            InventoryItemRepository inventoryRepository,
            StockMovementRepository movementRepository) {
        this.inventoryRepository = inventoryRepository;
        this.movementRepository = movementRepository;
    }

    @Transactional
    public synchronized InventoryItem addItem(InventoryItem item) {
        validateItem(item);
        if (inventoryRepository.existsById(item.getProductId())) {
            throw new IllegalStateException("Product ID already exists: " + item.getProductId());
        }
        item.setCategory(item.getCategory() != null ? item.getCategory() : "UNCATEGORIZED");
        if (inventoryRepository.countByCategory(item.getCategory()) >= MAX_CAPACITY_PER_CATEGORY) {
            throw new IllegalStateException("Category capacity exceeded");
        }

        int initialQuantity = item.getQuantity();
        item.setQuantity(0);
        InventoryItem saved = inventoryRepository.save(item);
        if (initialQuantity > 0) {
            stockIn(saved.getProductId(), initialQuantity, "system");
        }
        return inventoryRepository.findByProductId(saved.getProductId()).orElse(saved);
    }

    @Transactional
    public InventoryItem updateItem(String productId, InventoryItem changes) {
        validateItem(changes);
        InventoryItem item = inventoryRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new ItemNotFoundException(productId));

        if (changes.getExpirationDate() != null && changes.getExpirationDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Expiration date cannot be in the past");
        }
        if (changes.getProductId() != null && !productId.equals(changes.getProductId())) {
            throw new IllegalArgumentException("Product ID cannot be changed");
        }
        if (changes.getUnitPrice() < 0 || !Double.isFinite(changes.getUnitPrice())) {
            throw new IllegalArgumentException("Unit price cannot be negative");
        }
        if (changes.getReorderLevel() < 0) {
            throw new IllegalArgumentException("Reorder level cannot be negative");
        }
        if (changes.getCategory() != null
                && !changes.getCategory().isBlank()
                && !changes.getCategory().equals(item.getCategory())
                && inventoryRepository.countByCategory(changes.getCategory()) >= MAX_CAPACITY_PER_CATEGORY) {
            throw new IllegalStateException("Category capacity exceeded");
        }
        if (changes.getProductName() != null) {
            item.setProductName(changes.getProductName());
        }
        item.setExpirationDate(changes.getExpirationDate());
        if (changes.getCategory() != null && !changes.getCategory().isBlank()) {
            item.setCategory(changes.getCategory());
        }
        item.setUnitPrice(changes.getUnitPrice());
        item.setReorderLevel(changes.getReorderLevel());
        return inventoryRepository.save(item);
    }

    @Transactional
    public boolean deleteItem(String productId) {
        Optional<InventoryItem> item = inventoryRepository.findByProductIdForUpdate(productId);
        if (item.isEmpty()) {
            return false;
        }
        if (item.get().getQuantity() > 0) {
            stockOut(productId, item.get().getQuantity(), "system");
        }
        inventoryRepository.delete(item.get());
        return true;
    }

    @Transactional
    public StockMovement stockIn(String productId, int quantity, String user) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        InventoryItem item = inventoryRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new ItemNotFoundException(productId));
        try {
            item.setQuantity(Math.addExact(item.getQuantity(), quantity));
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Quantity exceeds the supported limit");
        }
        inventoryRepository.save(item);
        return movementRepository.save(new StockMovement(productId, MovementType.IN, quantity, normalizeUser(user)));
    }

    @Transactional
    public StockMovement stockOut(String productId, int quantity, String user) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        InventoryItem item = inventoryRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new ItemNotFoundException(productId));
        if (quantity > item.getQuantity()) {
            throw new IllegalArgumentException("Stock out quantity exceeds available quantity");
        }
        item.setQuantity(item.getQuantity() - quantity);
        inventoryRepository.save(item);
        return movementRepository.save(new StockMovement(productId, MovementType.OUT, quantity, normalizeUser(user)));
    }

    @Transactional
    public synchronized boolean removeItem(String productId, Integer requestedQuantity) {
        InventoryItem item = inventoryRepository.findByProductIdForUpdate(productId)
                .orElse(null);
        if (item == null) {
            return false;
        }
        int availableQuantity = item.getQuantity();
        int quantity = requestedQuantity == null || requestedQuantity <= 0
                ? availableQuantity
                : Math.min(requestedQuantity, availableQuantity);
        if (quantity > 0) {
            stockOut(productId, quantity, "system");
        }
        if (requestedQuantity == null || requestedQuantity <= 0 || requestedQuantity >= availableQuantity) {
            inventoryRepository.delete(item);
        }
        return true;
    }

    public List<InventoryItem> getAllItemsSortedByQuantity() {
        return inventoryRepository.findAllByOrderByQuantityAsc();
    }

    public List<InventoryItem> getAllItemsSortedByExpirationDate() {
        return inventoryRepository.findAllByOrderByExpirationDateAsc();
    }

    public List<InventoryItem> getItemsByCategory(String category) {
        return inventoryRepository.findByCategory(category);
    }

    public List<InventoryItem> getAllItems() {
        return inventoryRepository.findAll();
    }

    public Page<InventoryItem> searchItems(String search, String category, boolean lowStockOnly, Pageable pageable) {
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        String normalizedCategory = category == null || category.isBlank() ? null : category.trim();
        return inventoryRepository.searchItems(normalizedSearch, normalizedCategory, lowStockOnly, pageable);
    }

    public List<StockMovement> getMovements(String productId) {
        return movementRepository.findAllByProductIdOrderByMovementDateDesc(productId);
    }

    public List<StockMovement> getAllMovements() {
        return movementRepository.findAllByOrderByMovementDateDesc();
    }

    public Optional<InventoryItem> getItemById(String productId) {
        return inventoryRepository.findByProductId(productId);
    }

    public Map<String, InventoryItem[][]> getInventoryAs2DArray() {
        Map<String, InventoryItem[][]> result = new HashMap<>();
        Map<String, List<InventoryItem>> itemsByCategory = inventoryRepository.findAll().stream()
                .collect(Collectors.groupingBy(item -> item.getCategory() != null ? item.getCategory() : "UNCATEGORIZED"));
        for (Map.Entry<String, List<InventoryItem>> entry : itemsByCategory.entrySet()) {
            List<InventoryItem> items = entry.getValue();
            result.put(entry.getKey(), new InventoryItem[][]{items.toArray(new InventoryItem[0])});
        }
        return result;
    }

    public List<InventoryItem> getLowStockItems(Integer threshold) {
        if (threshold != null && threshold > 0) {
            return inventoryRepository.findByQuantityLessThanEqualOrderByQuantityAsc(threshold);
        }
        return inventoryRepository.findAll().stream()
                .filter(item -> item.getQuantity() <= item.getReorderLevel())
                .sorted((first, second) -> Integer.compare(first.getQuantity(), second.getQuantity()))
                .toList();
    }

    public Map<String, Object> getInventoryStatistics() {
        List<InventoryItem> allItems = inventoryRepository.findAll();
        int totalQuantity = allItems.stream().mapToInt(InventoryItem::getQuantity).sum();
        List<String> categories = allItems.stream()
                .map(InventoryItem::getCategory)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalItems", allItems.size());
        stats.put("totalQuantity", totalQuantity);
        stats.put("categoriesCount", categories.size());
        stats.put("lowStockCount", allItems.stream()
                .filter(item -> item.getQuantity() <= item.getReorderLevel()).count());
        stats.put("lowStockThreshold", DEFAULT_LOW_STOCK_THRESHOLD);
        stats.put("categories", categories);
        stats.put("totalStockValue", allItems.stream()
                .mapToDouble(item -> item.getQuantity() * item.getUnitPrice())
                .sum());
        stats.put("expiringCount", allItems.stream()
                .filter(item -> item.getExpirationDate() != null)
                .filter(item -> !item.getExpirationDate().isAfter(LocalDate.now().plusDays(30)))
                .count());
        Map<String, Integer> categoryStock = allItems.stream().collect(Collectors.groupingBy(
                item -> item.getCategory() != null ? item.getCategory() : "Uncategorized",
                Collectors.summingInt(InventoryItem::getQuantity)));
        stats.put("categoryStock", categoryStock);
        return stats;
    }

    @Transactional
    public synchronized void clearInventory() {
        inventoryRepository.deleteAll();
    }

    private void validateItem(InventoryItem item) {
        if (item == null || item.getProductId() == null || item.getProductId().isBlank()) {
            throw new IllegalArgumentException("Product ID is required");
        }
        if (item.getQuantity() < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        if (item.getUnitPrice() < 0 || !Double.isFinite(item.getUnitPrice())) {
            throw new IllegalArgumentException("Unit price cannot be negative");
        }
        if (item.getReorderLevel() < 0) {
            throw new IllegalArgumentException("Reorder level cannot be negative");
        }
        if (item.getExpirationDate() != null && item.getExpirationDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Expiration date cannot be in the past");
        }
    }

    private String normalizeUser(String user) {
        return user == null || user.isBlank() ? "system" : user.trim();
    }

    public static class ItemNotFoundException extends RuntimeException {
        public ItemNotFoundException(String productId) {
            super("Item not found: " + productId);
        }
    }
}
