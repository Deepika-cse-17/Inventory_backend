# Warehouse Inventory Management System

A dynamic inventory management system built with Java Spring Boot that uses 2D arrays for storing stock data with real-time addition, removal, and sorting capabilities.

## Features

- **Real-time Inventory Operations**: Add and remove items from inventory in real-time
- **2D Array Storage**: Stock data organized by category using 2D array structure
- **Smart Sorting**: Sort inventory by quantity or expiration date
- **RESTful API**: Complete REST API for all inventory operations
- **Web Interface**: Beautiful HTML frontend with modern UI for managing inventory
- **Category-based Organization**: Items organized by categories for efficient management
- **Quantity Tracking**: Track quantities per product with automatic updates

## Technology Stack

- **Java 17**
- **Spring Boot 3.1.5**
- **Maven** for dependency management

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/warehouse/inventory/
│   │       ├── InventoryManagementApplication.java
│   │       ├── controller/
│   │       │   └── InventoryController.java
│   │       ├── model/
│   │       │   └── InventoryItem.java
│   │       └── service/
│   │           └── InventoryService.java
│   └── resources/
│       └── application.properties
└── pom.xml
```

## API Endpoints

### Add Item
```http
POST /api/inventory/add
Content-Type: application/json

{
  "productId": "PROD001",
  "productName": "Widget A",
  "quantity": 100,
  "expirationDate": "2024-12-31",
  "category": "Electronics",
  "unitPrice": 29.99
}
```

### Remove Item
```http
DELETE /api/inventory/remove/{productId}?quantity={quantity}
```
- If `quantity` is provided, reduces the quantity
- If `quantity` is omitted or 0, removes the entire item

### Get All Items
```http
GET /api/inventory/all
```

### Get Items Sorted by Quantity
```http
GET /api/inventory/sorted/quantity
```

### Get Items Sorted by Expiration Date
```http
GET /api/inventory/sorted/expiration
```

### Get Items by Category
```http
GET /api/inventory/category/{category}
```

### Get Item by ID
```http
GET /api/inventory/{productId}
```

### Get Inventory as 2D Array
```http
GET /api/inventory/2d-array
```
Returns inventory in 2D array format organized by category.

### Get Statistics
```http
GET /api/inventory/statistics
```

## Running the Application

### Option 1: Using Docker (Recommended)

1. **Prerequisites**:
   - Docker installed and running
   - Docker Compose (optional, for easier deployment)

2. **Build and run with Docker**:
   ```bash
   docker build -t inventory-management .
   docker run -p 8080:8080 inventory-management
   ```

3. **Or use Docker Compose**:
   ```bash
   docker-compose up -d
   ```

4. **Access the Application**:
   - Server runs on `http://localhost:8080`
   - **Web Interface**: Visit `http://localhost:8080/` or `http://localhost:8080/index.html`
   - API endpoints available at `http://localhost:8080/api/inventory/*`

5. **Stop the container**:
   ```bash
   docker-compose down
   # or
   docker stop inventory-management-app
   ```

### Option 2: Local Development

1. **Prerequisites**:
   - Java 17 or higher
   - Maven 3.6+

2. **Build the project**:
   ```bash
   mvn clean install
   ```

3. **Run the application**:
   ```bash
   mvn spring-boot:run
   ```

4. **Access the Application**:
   - Server runs on `http://localhost:8080`
   - **Web Interface**: Visit `http://localhost:8080/` or `http://localhost:8080/index.html`
   - API endpoints available at `http://localhost:8080/api/inventory/*`

## Example Usage

### Adding an item:
```bash
curl -X POST http://localhost:8080/api/inventory/add \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "PROD001",
    "productName": "Widget A",
    "quantity": 100,
    "expirationDate": "2024-12-31",
    "category": "Electronics",
    "unitPrice": 29.99
  }'
```

### Getting items sorted by expiration date:
```bash
curl http://localhost:8080/api/inventory/sorted/expiration
```

### Removing an item:
```bash
curl -X DELETE http://localhost:8080/api/inventory/remove/PROD001
```

## Web Interface Features

The HTML frontend (`index.html`) provides:

- **Add Items**: Form to add new inventory items with all required fields
- **View Inventory**: Table display of all items with sorting and filtering
- **Sort Options**: 
  - Sort by quantity (ascending)
  - Sort by expiration date (earliest first)
- **Category Filter**: Filter items by category
- **Statistics Dashboard**: Real-time statistics showing:
  - Total number of items
  - Total quantity across all items
  - Number of categories
- **Remove Items**: 
  - Remove partial quantities
  - Remove entire items
- **Expiration Alerts**: Items past expiration date are highlighted in red
- **Responsive Design**: Works on desktop and mobile devices

## Data Model

### InventoryItem
- `productId` (String): Unique identifier for the product
- `productName` (String): Name of the product
- `quantity` (int): Current stock quantity
- `expirationDate` (LocalDate): Expiration date of the product
- `category` (String): Category/classification of the product
- `unitPrice` (double): Price per unit

## 2D Array Implementation

The inventory is stored using a 2D array concept where:
- **First dimension**: Categories (e.g., "Electronics", "Food", "Clothing")
- **Second dimension**: Items within each category

The system maintains this structure using a `Map<String, List<InventoryItem>>` which allows for:
- Dynamic sizing per category
- Efficient category-based operations
- Easy conversion to actual 2D arrays when needed

## Thread Safety

All inventory operations are synchronized to ensure thread-safe real-time updates in a multi-threaded environment.

