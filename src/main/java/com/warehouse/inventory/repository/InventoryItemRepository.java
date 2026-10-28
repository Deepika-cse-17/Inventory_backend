package com.warehouse.inventory.repository;

import com.warehouse.inventory.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, String> {
    List<InventoryItem> findAllByOrderByQuantityAsc();

    List<InventoryItem> findAllByOrderByExpirationDateAsc();

    List<InventoryItem> findByCategory(String category);

    Optional<InventoryItem> findByProductId(String productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT item FROM InventoryItem item WHERE item.productId = :productId")
    Optional<InventoryItem> findByProductIdForUpdate(@Param("productId") String productId);

    List<InventoryItem> findByQuantityLessThanEqualOrderByQuantityAsc(int quantity);

    long countByCategory(String category);

    @Query("SELECT item FROM InventoryItem item WHERE "
            + "(:search IS NULL OR LOWER(item.productId) LIKE LOWER(CONCAT('%', :search, '%')) "
            + "OR LOWER(item.productName) LIKE LOWER(CONCAT('%', :search, '%'))) "
            + "AND (:category IS NULL OR item.category = :category) "
            + "AND (:lowStockOnly = false OR item.quantity <= item.reorderLevel)")
    Page<InventoryItem> searchItems(
            @Param("search") String search,
            @Param("category") String category,
            @Param("lowStockOnly") boolean lowStockOnly,
            Pageable pageable);

}