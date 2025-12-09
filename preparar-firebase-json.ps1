# Script para convertir serviceAccountKey.json a formato de variable de entorno
# Este script lee el archivo JSON y lo convierte en una sola línea para Render.com

Write-Host "================================================"
Write-Host "Preparar Firebase Credentials para Render.com"
Write-Host "================================================"
Write-Host ""

$jsonPath = "serviceAccountKey.json"

if (-not (Test-Path $jsonPath)) {
    Write-Host "ERROR: No se encuentra el archivo $jsonPath" -ForegroundColor Red
    Write-Host "Asegúrate de estar en el directorio backend/" -ForegroundColor Yellow
    exit 1
}

Write-Host "Leyendo archivo: $jsonPath" -ForegroundColor Green
$jsonContent = Get-Content $jsonPath -Raw

# Convertir a una sola línea (eliminar saltos de línea y espacios extras)
$singleLine = $jsonContent -replace "`r`n", "" -replace "`n", "" -replace "`r", "" -replace "\s+", " "

Write-Host ""
Write-Host "================================================"
Write-Host "Copia este valor para FIREBASE_CREDENTIALS_JSON en Render.com:"
Write-Host "================================================"
Write-Host ""
Write-Host $singleLine -ForegroundColor Cyan
Write-Host ""
Write-Host "================================================"
Write-Host "Instrucciones:"
Write-Host "1. Ve a tu servicio en Render.com"
Write-Host "2. Ve a 'Environment'"
Write-Host "3. Agrega la variable: FIREBASE_CREDENTIALS_JSON"
Write-Host "4. Pega el valor de arriba"
Write-Host "================================================"

