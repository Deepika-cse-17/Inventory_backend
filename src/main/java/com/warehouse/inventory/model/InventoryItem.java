package com.warehouse.inventory.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents an inventory item with product information, quantity, and expiration date.
 */
public class InventoryItem {
    private String productId;
    private String productName;
    private int quantity;
    private LocalDate expirationDate;
    private String category;
    private double unitPrice;

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

