# API Usage Examples

This document provides practical examples for using the Inventory Management API.

## Prerequisites
- Server running on `http://localhost:8080`
- Use any REST client (Postman, cURL, or HTTPie)

## 1. Add Items to Inventory

### Example 1: Add Electronics Item
```bash
curl -X POST http://localhost:8080/api/inventory/add \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "ELEC001",
    "productName": "Laptop Computer",
    "quantity": 50,
    "expirationDate": "2025-06-30",
    "category": "Electronics",
    "unitPrice": 999.99
  }'
```

### Example 2: Add Food Item
```bash
curl -X POST http://localhost:8080/api/inventory/add \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "FOOD001",
    "productName": "Canned Beans",
    "quantity": 200,
    "expirationDate": "2024-03-15",
    "category": "Food",
    "unitPrice": 2.49
  }'
```

### Example 3: Add Clothing Item
```bash
curl -X POST http://localhost:8080/api/inventory/add \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "CLOTH001",
    "productName": "Cotton T-Shirt",
    "quantity": 150,
    "expirationDate": null,
    "category": "Clothing",
    "unitPrice": 19.99
  }'
```

## 2. Update Item Quantity (Add More Stock)

Adding an item with the same `productId` will update the quantity:
```bash
curl -X POST http://localhost:8080/api/inventory/add \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "ELEC001",
    "productName": "Laptop Computer",
    "quantity": 25,
    "expirationDate": "2025-06-30",
    "category": "Electronics",
    "unitPrice": 999.99
  }'
```
This will increase the quantity from 50 to 75.

## 3. Remove Items

### Remove Entire Item
```bash
curl -X DELETE http://localhost:8080/api/inventory/remove/ELEC001
```

### Remove Partial Quantity
```bash
curl -X DELETE "http://localhost:8080/api/inventory/remove/FOOD001?quantity=50"
```
This reduces the quantity by 50 (from 200 to 150 in our example).

## 4. View All Items

```bash
curl http://localhost:8080/api/inventory/all
```

## 5. Sort Items by Quantity

```bash
curl http://localhost:8080/api/inventory/sorted/quantity
```
Returns items sorted by quantity (ascending - lowest to highest).

## 6. Sort Items by Expiration Date

```bash
curl http://localhost:8080/api/inventory/sorted/expiration
```
Returns items sorted by expiration date (ascending - earliest expiration first).

## 7. Get Items by Category

```bash
curl http://localhost:8080/api/inventory/category/Electronics
```

## 8. Get Specific Item

```bash
curl http://localhost:8080/api/inventory/ELEC001
```

## 9. View Inventory Statistics

```bash
curl http://localhost:8080/api/inventory/statistics
```

Response example:
```json
{
  "success": true,
  "statistics": {
    "totalItems": 3,
    "totalQuantity": 400,
    "categoriesCount": 3,
    "categories": ["Electronics", "Food", "Clothing"]
  }
}
```

## 10. Get Inventory as 2D Array

```bash
curl http://localhost:8080/api/inventory/2d-array
```

This returns the inventory structure as a 2D array organized by category.

## Complete Workflow Example

```bash
# 1. Add multiple items
curl -X POST http://localhost:8080/api/inventory/add \
  -H "Content-Type: application/json" \
  -d '{"productId":"ITEM1","productName":"Product 1","quantity":100,"expirationDate":"2024-12-31","category":"A","unitPrice":10.0}'

curl -X POST http://localhost:8080/api/inventory/add \
  -H "Content-Type: application/json" \
  -d '{"productId":"ITEM2","productName":"Product 2","quantity":50,"expirationDate":"2024-06-30","category":"A","unitPrice":20.0}'

curl -X POST http://localhost:8080/api/inventory/add \
  -H "Content-Type: application/json" \
  -d '{"productId":"ITEM3","productName":"Product 3","quantity":200,"expirationDate":"2024-09-15","category":"B","unitPrice":15.0}'

# 2. View all items sorted by quantity
curl http://localhost:8080/api/inventory/sorted/quantity

# 3. View all items sorted by expiration date
curl http://localhost:8080/api/inventory/sorted/expiration

# 4. Check statistics
curl http://localhost:8080/api/inventory/statistics

# 5. Remove some quantity from an item
curl -X DELETE "http://localhost:8080/api/inventory/remove/ITEM1?quantity=25"

# 6. View updated inventory
curl http://localhost:8080/api/inventory/all
```

## Response Format

All successful responses follow this format:
```json
{
  "success": true,
  "message": "Optional message",
  "data": { ... }
}
```

Error responses:
```json
{
  "success": false,
  "message": "Error description"
}
```

