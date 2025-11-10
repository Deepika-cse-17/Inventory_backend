@echo off
echo Building Docker image...
docker build -t inventory-management:latest .

if %ERRORLEVEL% EQU 0 (
    echo Build successful! Starting container...
    
    REM Stop and remove existing container if it exists
    docker stop inventory-management-app 2>nul
    docker rm inventory-management-app 2>nul
    
    REM Run the container
    docker run -d -p 8080:8080 --name inventory-management-app --restart unless-stopped inventory-management:latest
    
    if %ERRORLEVEL% EQU 0 (
        echo.
        echo Container started successfully!
        echo Application is available at: http://localhost:8080
        echo.
        echo Checking container status...
        timeout /t 3 /nobreak >nul
        docker ps | findstr inventory-management
        echo.
        echo To view logs, run: docker logs -f inventory-management-app
    ) else (
        echo Failed to start container
    )
) else (
    echo Build failed!
)

