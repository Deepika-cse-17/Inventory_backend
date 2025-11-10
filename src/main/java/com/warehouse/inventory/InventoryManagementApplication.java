package com.warehouse.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class InventoryManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryManagementApplication.class, args);
        System.out.println("===========================================");
        System.out.println("Warehouse Inventory Management System");
        System.out.println("Server started on http://localhost:8080");
        System.out.println("API Documentation:");
        System.out.println("  POST   /api/inventory/add");
        System.out.println("  DELETE /api/inventory/remove/{productId}");
        System.out.println("  GET    /api/inventory/all");
        System.out.println("  GET    /api/inventory/sorted/quantity");
        System.out.println("  GET    /api/inventory/sorted/expiration");
        System.out.println("  GET    /api/inventory/statistics");
        System.out.println("===========================================");
    }
}

