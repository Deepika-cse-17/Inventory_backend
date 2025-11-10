package com.warehouse.inventory.service;

import com.warehouse.inventory.model.InventoryItem;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class managing inventory operations using 2D array structure.
 * The 2D array represents: [category][items] where items are stored by category.
 */
@Service
public class InventoryService {
    
    // Using Map to represent 2D structure: category -> List of items
    // This allows dynamic sizing per category while maintaining 2D array concept
    private final Map<String, List<InventoryItem>> inventoryStore;
    
    // Maximum capacity per category (can be configured)
    private static final int MAX_CAPACITY_PER_CATEGORY = 1000;
    
    // Default low stock threshold (can be configured per item or globally)
    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 10;
    
    public InventoryService() {
        this.inventoryStore = new HashMap<>();
    }

    /**
     * Adds a new item to inventory or updates quantity if item exists.
     * @param item The inventory item to add
     * @return true if added successfully, false if capacity exceeded
     */
    public synchronized boolean addItem(InventoryItem item) {
        if (item == null || item.getProductId() == null) {
            throw new IllegalArgumentException("Item and productId cannot be null");
        }

        String category = item.getCategory() != null ? item.getCategory() : "UNCATEGORIZED";
        List<InventoryItem> categoryItems = inventoryStore.computeIfAbsent(category, k -> new ArrayList<>());

        // Check if item already exists in this category
        OptionalInt indexOpt = findItemIndex(categoryItems, item.getProductId());
        
        if (indexOpt.isPresent()) {
            // Update quantity of existing item
            int index = indexOpt.getAsInt();
            InventoryItem existing = categoryItems.get(index);
            existing.setQuantity(existing.getQuantity() + item.getQuantity());
            // Update other fields if provided
            if (item.getProductName() != null) existing.setProductName(item.getProductName());
            if (item.getExpirationDate() != null) existing.setExpirationDate(item.getExpirationDate());
            if (item.getUnitPrice() > 0) existing.setUnitPrice(item.getUnitPrice());
            return true;
        } else {
            // Add new item
            if (categoryItems.size() >= MAX_CAPACITY_PER_CATEGORY) {
                return false;
            }
            categoryItems.add(item);
            return true;
        }
    }

    /**
     * Removes an item from inventory by productId.
     * @param productId The product ID to remove
     * @param quantity Quantity to remove (if null or 0, removes entire item)
     * @return true if removed successfully, false if not found
     */
    public synchronized boolean removeItem(String productId, Integer quantity) {
        if (productId == null) {
            throw new IllegalArgumentException("ProductId cannot be null");
        }

        for (List<InventoryItem> categoryItems : inventoryStore.values()) {
            OptionalInt indexOpt = findItemIndex(categoryItems, productId);
            
            if (indexOpt.isPresent()) {
                int index = indexOpt.getAsInt();
                InventoryItem item = categoryItems.get(index);
                
                if (quantity == null || quantity <= 0 || quantity >= item.getQuantity()) {
                    // Remove entire item
                    categoryItems.remove(index);
                } else {
                    // Reduce quantity
                    item.setQuantity(item.getQuantity() - quantity);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Gets all items sorted by quantity (ascending).
     * @return List of items sorted by quantity
     */
    public List<InventoryItem> getAllItemsSortedByQuantity() {
        return inventoryStore.values().stream()
                .flatMap(List::stream)
                .sorted(Comparator.comparingInt(InventoryItem::getQuantity))
                .collect(Collectors.toList());
    }

    /**
     * Gets all items sorted by expiration date (ascending - earliest first).
     * @return List of items sorted by expiration date
     */
    public List<InventoryItem> getAllItemsSortedByExpirationDate() {
        return inventoryStore.values().stream()
                .flatMap(List::stream)
                .sorted(Comparator.comparing(InventoryItem::getExpirationDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    /**
     * Gets all items from a specific category.
     * @param category The category name
     * @return List of items in the category
     */
    public List<InventoryItem> getItemsByCategory(String category) {
        return inventoryStore.getOrDefault(category, new ArrayList<>());
    }

    /**
     * Gets all items across all categories.
     * @return List of all items
     */
    public List<InventoryItem> getAllItems() {
        return inventoryStore.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    /**
     * Gets a specific item by productId.
     * @param productId The product ID
     * @return Optional containing the item if found
     */
    public Optional<InventoryItem> getItemById(String productId) {
        return inventoryStore.values().stream()
                .flatMap(List::stream)
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();
    }

    /**
     * Gets the 2D array representation of inventory.
     * @return Map where key is category and value is array of items
     */
    public Map<String, InventoryItem[][]> getInventoryAs2DArray() {
        Map<String, InventoryItem[][]> result = new HashMap<>();
        for (Map.Entry<String, List<InventoryItem>> entry : inventoryStore.entrySet()) {
            List<InventoryItem> items = entry.getValue();
            InventoryItem[][] array = new InventoryItem[1][items.size()];
            array[0] = items.toArray(new InventoryItem[0]);
            result.put(entry.getKey(), array);
        }
        return result;
    }

    /**
     * Gets items with low stock (quantity <= threshold).
     * @param threshold The threshold for low stock (default: 10)
     * @return List of items with low stock
     */
    public List<InventoryItem> getLowStockItems(Integer threshold) {
        int stockThreshold = threshold != null && threshold > 0 ? threshold : DEFAULT_LOW_STOCK_THRESHOLD;
        return inventoryStore.values().stream()
                .flatMap(List::stream)
                .filter(item -> item.getQuantity() <= stockThreshold)
                .sorted(Comparator.comparingInt(InventoryItem::getQuantity))
                .collect(Collectors.toList());
    }

    /**
     * Gets inventory statistics.
     * @return Map containing statistics
     */
    public Map<String, Object> getInventoryStatistics() {
        int totalItems = inventoryStore.values().stream()
                .mapToInt(List::size)
                .sum();
        int totalQuantity = inventoryStore.values().stream()
                .flatMap(List::stream)
                .mapToInt(InventoryItem::getQuantity)
                .sum();
        int categoriesCount = inventoryStore.size();
        List<InventoryItem> lowStockItems = getLowStockItems(DEFAULT_LOW_STOCK_THRESHOLD);
        int lowStockCount = lowStockItems.size();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalItems", totalItems);
        stats.put("totalQuantity", totalQuantity);
        stats.put("categoriesCount", categoriesCount);
        stats.put("lowStockCount", lowStockCount);
        stats.put("lowStockThreshold", DEFAULT_LOW_STOCK_THRESHOLD);
        stats.put("categories", new ArrayList<>(inventoryStore.keySet()));
        
        return stats;
    }

    /**
     * Helper method to find item index in a category list.
     */
    private OptionalInt findItemIndex(List<InventoryItem> items, String productId) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getProductId().equals(productId)) {
                return OptionalInt.of(i);
            }
        }
        return OptionalInt.empty();
    }

    /**
     * Clears all inventory (for testing/reset purposes).
     */
    public synchronized void clearInventory() {
        inventoryStore.clear();
    }
}

