# Script de prueba de conexión del backend
# Este script verifica que el backend esté funcionando correctamente

Write-Host "=== Prueba de Conexión Backend ===" -ForegroundColor Cyan
Write-Host ""

# Verificar que el backend esté corriendo
Write-Host "1. Verificando si el backend está corriendo en http://localhost:8080..." -ForegroundColor Yellow

try {
    $healthResponse = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -Method GET -TimeoutSec 5 -ErrorAction Stop
    Write-Host "   ✓ Backend está corriendo" -ForegroundColor Green
    Write-Host "   Respuesta: $($healthResponse.StatusCode)" -ForegroundColor Gray
} catch {
    Write-Host "   ✗ Backend no está corriendo o no responde" -ForegroundColor Red
    Write-Host "   Asegúrate de iniciar el backend con: ./gradlew bootRun" -ForegroundColor Yellow
    exit 1
}

Write-Host ""

# Probar endpoint de refresh token
Write-Host "2. Probando endpoint de refresh token..." -ForegroundColor Yellow

$refreshTokenBody = @{
    refreshToken = "demo-refresh-token"
} | ConvertTo-Json

try {
    $refreshResponse = Invoke-RestMethod -Uri "http://localhost:8080/v1/auth/refresh" -Method POST -Body $refreshTokenBody -ContentType "application/json" -TimeoutSec 10
    Write-Host "   ✓ Refresh token funcionando" -ForegroundColor Green
    Write-Host "   Respuesta:" -ForegroundColor Gray
    Write-Host "   $($refreshResponse | ConvertTo-Json -Depth 3)" -ForegroundColor Gray
} catch {
    Write-Host "   ✗ Error en refresh token" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "   Respuesta del servidor: $responseBody" -ForegroundColor Red
    }
    exit 1
}

Write-Host ""
Write-Host "=== Pruebas completadas exitosamente ===" -ForegroundColor Green
Write-Host ""
Write-Host "El backend está listo para recibir conexiones desde Android." -ForegroundColor Cyan
Write-Host "URL base: http://localhost:8080/v1/" -ForegroundColor Cyan
Write-Host "Para Android emulador: http://10.0.2.2:8080/v1/" -ForegroundColor Cyan




