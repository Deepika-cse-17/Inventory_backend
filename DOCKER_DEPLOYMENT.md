# Docker Deployment Guide

This guide explains how to deploy the Warehouse Inventory Management System using Docker.

## Prerequisites

- Docker installed and running
- Docker Compose (optional, but recommended)

## Quick Start

### Using Docker Compose (Easiest)

1. **Build and start the application**:
   ```bash
   docker-compose up -d
   ```

2. **View logs**:
   ```bash
   docker-compose logs -f
   ```

3. **Stop the application**:
   ```bash
   docker-compose down
   ```

4. **Rebuild and restart**:
   ```bash
   docker-compose up -d --build
   ```

### Using Docker Commands

1. **Build the Docker image**:
   ```bash
   docker build -t inventory-management:latest .
   ```

2. **Run the container**:
   ```bash
   docker run -d -p 8080:8080 --name inventory-management-app inventory-management:latest
   ```

3. **View logs**:
   ```bash
   docker logs -f inventory-management-app
   ```

4. **Stop the container**:
   ```bash
   docker stop inventory-management-app
   ```

5. **Remove the container**:
   ```bash
   docker rm inventory-management-app
   ```

## Dockerfile Details

The Dockerfile uses a multi-stage build:

1. **Build Stage**: Uses Maven to compile and package the application
2. **Runtime Stage**: Uses a lightweight JRE image to run the application

This approach results in a smaller final image size.

## Configuration

### Environment Variables

You can customize the application using environment variables:

```bash
docker run -d -p 8080:8080 \
  -e SERVER_PORT=8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  --name inventory-management-app \
  inventory-management:latest
```

### Port Mapping

The default port is 8080. To use a different port:

```bash
docker run -d -p 9090:8080 --name inventory-management-app inventory-management:latest
```

Then access the application at `http://localhost:9090`

## Health Check

The container includes a health check that verifies the application is running:

```bash
docker ps
```

Check the STATUS column - it should show "healthy" after the application starts.

## Troubleshooting

### Container won't start

1. **Check logs**:
   ```bash
   docker logs inventory-management-app
   ```

2. **Check if port is already in use**:
   ```bash
   netstat -ano | findstr :8080
   ```

3. **Verify Docker is running**:
   ```bash
   docker ps
   ```

### Application is slow to start

The application may take 30-40 seconds to start. This is normal for Spring Boot applications. The health check will wait up to 40 seconds before marking the container as unhealthy.

### Rebuild after code changes

If you've made code changes:

```bash
docker-compose up -d --build
```

Or with Docker:

```bash
docker build -t inventory-management:latest .
docker stop inventory-management-app
docker rm inventory-management-app
docker run -d -p 8080:8080 --name inventory-management-app inventory-management:latest
```

## Production Deployment

For production deployment, consider:

1. **Use a specific version tag**:
   ```bash
   docker build -t inventory-management:v1.0.0 .
   ```

2. **Set resource limits**:
   ```yaml
   # In docker-compose.yml
   deploy:
     resources:
       limits:
         cpus: '1'
         memory: 512M
   ```

3. **Use environment-specific configuration**:
   ```bash
   docker run -d -p 8080:8080 \
     -e SPRING_PROFILES_ACTIVE=prod \
     -v /path/to/application-prod.properties:/app/config/application.properties \
     inventory-management:latest
   ```

4. **Set up logging**:
   ```bash
   docker run -d -p 8080:8080 \
     -v /path/to/logs:/app/logs \
     inventory-management:latest
   ```

## Docker Compose Configuration

The `docker-compose.yml` file includes:

- **Port mapping**: Maps container port 8080 to host port 8080
- **Health check**: Monitors application health
- **Restart policy**: Automatically restarts on failure
- **Network**: Creates an isolated network for the application

## Accessing the Application

Once the container is running:

- **Web Interface**: `http://localhost:8080/`
- **API Base URL**: `http://localhost:8080/api/inventory/`
- **Statistics**: `http://localhost:8080/api/inventory/statistics`

## Stopping and Cleaning Up

```bash
# Stop and remove containers
docker-compose down

# Remove images
docker rmi inventory-management:latest

# Remove all unused Docker resources
docker system prune -a
```

