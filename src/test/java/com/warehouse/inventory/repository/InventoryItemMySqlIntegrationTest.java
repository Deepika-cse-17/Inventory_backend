package com.warehouse.inventory.repository;

import com.warehouse.inventory.model.InventoryItem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers(disabledWithoutDocker = true)
@SuppressWarnings("resource")
class InventoryItemMySqlIntegrationTest {
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("inventory_test")
            .withUsername("inventory_test")
            .withPassword("inventory_test_password");

    @BeforeAll
    static void startMySql() {
        MYSQL.start();
    }

    @AfterAll
    static void stopMySql() {
        MYSQL.close();
    }

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("app.auth.admin.password", () -> "test-admin-password");
    }

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void savesInventoryItemToMySqlAndReadsItBack() {
        InventoryItem item = new InventoryItem(
                "MYSQL-101", "Test Coffee", 18, LocalDate.now().plusDays(90), "Beverages", 12.75);
        item.setReorderLevel(6);

        inventoryItemRepository.saveAndFlush(item);
        entityManager.clear();

        Optional<InventoryItem> result = inventoryItemRepository.findById("MYSQL-101");

        assertTrue(result.isPresent());
        assertEquals("Test Coffee", result.get().getProductName());
        assertEquals(18, result.get().getQuantity());
        assertEquals(6, result.get().getReorderLevel());
    }
}
