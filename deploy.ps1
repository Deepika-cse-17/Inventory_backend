# PowerShell script to deploy inventory management system
Write-Host "Building Docker image..." -ForegroundColor Yellow
docker build -t inventory-management:latest .

if ($LASTEXITCODE -eq 0) {
    Write-Host "Build successful! Starting container..." -ForegroundColor Green
    
    # Stop and remove existing container if it exists
    docker stop inventory-management-app 2>$null
    docker rm inventory-management-app 2>$null
    
    # Run the container
    docker run -d -p 8080:8080 --name inventory-management-app --restart unless-stopped inventory-management:latest
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "`nContainer started successfully!" -ForegroundColor Green
        Write-Host "Application is available at: http://localhost:8080" -ForegroundColor Cyan
        Write-Host "`nChecking container status..." -ForegroundColor Yellow
        Start-Sleep -Seconds 3
        docker ps | Select-String "inventory-management"
        Write-Host "`nTo view logs, run: docker logs -f inventory-management-app" -ForegroundColor Yellow
    } else {
        Write-Host "Failed to start container" -ForegroundColor Red
    }
} else {
    Write-Host "Build failed!" -ForegroundColor Red
}

