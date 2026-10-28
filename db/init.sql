CREATE TABLE IF NOT EXISTS inventory_items (
    product_id VARCHAR(100) NOT NULL,
    product_name VARCHAR(255),
    quantity INT NOT NULL,
    expiration_date DATE,
    category VARCHAR(100),
    unit_price DOUBLE NOT NULL,
    reorder_level INT NOT NULL DEFAULT 10,
    PRIMARY KEY (product_id)
);

CREATE TABLE IF NOT EXISTS stock_movements (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id VARCHAR(100) NOT NULL,
    movement_type VARCHAR(3) NOT NULL,
    quantity INT NOT NULL,
    movement_date DATETIME NOT NULL,
    performed_by VARCHAR(100) NOT NULL DEFAULT 'system',
    PRIMARY KEY (id),
    INDEX idx_stock_movements_product_date (product_id, movement_date)
);

CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL,
    password VARCHAR(100) NOT NULL,
    `role` VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username)
);

CREATE TABLE IF NOT EXISTS contact_messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL,
    message VARCHAR(4000) NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id)
);
