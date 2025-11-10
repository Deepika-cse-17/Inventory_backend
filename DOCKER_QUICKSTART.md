# Quick Start Guide - Docker Deployment

## Prerequisites
- Docker installed and running
- Docker Compose (optional)

## Quick Start (3 Steps)

### Step 1: Build the Docker Image
```bash
docker build -t inventory-management .
```

### Step 2: Run the Container
```bash
docker run -d -p 8080:8080 --name inventory-management-app inventory-management
```

### Step 3: Access the Application
Open your browser and visit:
- **Web Interface**: http://localhost:8080
- **API**: http://localhost:8080/api/inventory/statistics

## Using Docker Compose (Even Easier)

### Single Command to Start Everything:
```bash
docker-compose up -d
```

### View Logs:
```bash
docker-compose logs -f
```

### Stop Everything:
```bash
docker-compose down
```

## Verify It's Running

Check container status:
```bash
docker ps
```

You should see `inventory-management-app` with status "Up" and health check passing.

## Common Commands

```bash
# View logs
docker logs -f inventory-management-app

# Stop container
docker stop inventory-management-app

# Start container
docker start inventory-management-app

# Remove container
docker rm inventory-management-app

# Rebuild after code changes
docker build -t inventory-management . && docker restart inventory-management-app
```

## Troubleshooting

### Port Already in Use
If port 8080 is already in use, change the port:
```bash
docker run -d -p 9090:8080 --name inventory-management-app inventory-management
```
Then access at http://localhost:9090

### Container Won't Start
Check logs:
```bash
docker logs inventory-management-app
```

### Rebuild Everything
```bash
docker-compose down
docker-compose up -d --build
```

