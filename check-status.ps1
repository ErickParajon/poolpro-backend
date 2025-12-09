Write-Host "Verificando estado del backend..." -ForegroundColor Cyan
Start-Sleep -Seconds 5

try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -UseBasicParsing -TimeoutSec 5
    Write-Host "Backend esta corriendo!" -ForegroundColor Green
    Write-Host "Status: $($response.StatusCode)" -ForegroundColor Gray
    Write-Host "Respuesta: $($response.Content)" -ForegroundColor Gray
} catch {
    Write-Host "El backend aun no responde" -ForegroundColor Yellow
    Write-Host "Puede estar:" -ForegroundColor Yellow
    Write-Host "  - Compilando (puede tardar varios minutos la primera vez)" -ForegroundColor Gray
    Write-Host "  - Esperando conexion a PostgreSQL" -ForegroundColor Gray
    Write-Host "  - Iniciando servicios..." -ForegroundColor Gray
    Write-Host ""
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "Verifica que PostgreSQL este corriendo:" -ForegroundColor Cyan
    Write-Host "  docker ps | findstr poolpro-db" -ForegroundColor Gray
}
