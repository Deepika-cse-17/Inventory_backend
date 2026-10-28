package com.warehouse.inventory.model;

import java.time.LocalDate;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Represents an inventory item with product information, quantity, and expiration date.
 */
@Entity
@Table(name = "inventory_items")
public class InventoryItem {
    @Id
    @Column(name = "product_id", nullable = false, length = 100)
    @NotBlank(message = "Product ID is required")
    @Size(max = 100, message = "Product ID cannot exceed 100 characters")
    private String productId;
    @Column(name = "product_name", length = 255)
    @Size(max = 255, message = "Product name cannot exceed 255 characters")
    private String productName;
    @Column(nullable = false)
    @PositiveOrZero(message = "Quantity cannot be negative")
    private int quantity;
    @Column(name = "expiration_date")
    @FutureOrPresent(message = "Expiration date cannot be in the past")
    private LocalDate expirationDate;
    @Column(length = 100)
    @Size(max = 100, message = "Category cannot exceed 100 characters")
    private String category;
    @Column(name = "unit_price", nullable = false)
    @DecimalMin(value = "0.0", message = "Unit price cannot be negative")
    private double unitPrice;
    @Column(name = "reorder_level", nullable = false, columnDefinition = "int not null default 10")
    @PositiveOrZero(message = "Reorder level cannot be negative")
    private int reorderLevel = 10;

    public InventoryItem() {
    }

    public InventoryItem(String productId, String productName, int quantity, LocalDate expirationDate, String category, double unitPrice) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.expirationDate = expirationDate;
        this.category = category;
        this.unitPrice = unitPrice;
    }

    // Getters and Setters
    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    @JsonProperty("name")
    public String getName() {
        return productName;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.productName = name;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(int reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InventoryItem that = (InventoryItem) o;
        return Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId);
    }

    @Override
    public String toString() {
        return "InventoryItem{" +
                "productId='" + productId + '\'' +
                ", productName='" + productName + '\'' +
                ", quantity=" + quantity +
                ", expirationDate=" + expirationDate +
                ", category='" + category + '\'' +
                ", unitPrice=" + unitPrice +
                '}';
    }
}
