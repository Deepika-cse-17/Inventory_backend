package com.warehouse.inventory.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movements", indexes = @Index(
        name = "idx_stock_movements_product_date",
        columnList = "product_id,movement_date"))
public class StockMovement {
    public enum MovementType {
        IN, OUT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false, length = 100)
    private String productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 3)
    private MovementType type;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "movement_date", nullable = false)
    private LocalDateTime movementDate;

    @Column(name = "performed_by", nullable = false, length = 100)
    private String performedBy;

    protected StockMovement() {
    }

    public StockMovement(String productId, MovementType type, int quantity, String performedBy) {
        this.productId = productId;
        this.type = type;
        this.quantity = quantity;
        this.movementDate = LocalDateTime.now();
        this.performedBy = performedBy;
    }

    public Long getId() {
        return id;
    }

    public String getProductId() {
        return productId;
    }

    public MovementType getType() {
        return type;
    }

    public int getQuantity() {
        return quantity;
    }

    public LocalDateTime getMovementDate() {
        return movementDate;
    }

    @JsonProperty("user")
    public String getPerformedBy() {
        return performedBy;
    }
}
