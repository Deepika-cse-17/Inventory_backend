package com.warehouse.inventory.repository;

import com.warehouse.inventory.model.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findAllByProductIdOrderByMovementDateDesc(String productId);

    List<StockMovement> findAllByOrderByMovementDateDesc();
}
