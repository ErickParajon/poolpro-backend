# Script para iniciar el backend con Firebase
# Asegúrate de que Docker Desktop esté corriendo y PostgreSQL esté iniciado

Write-Host "=== Iniciando Backend PoolProManager ===" -ForegroundColor Cyan
Write-Host ""

# Verificar variables de entorno
$projectId = [System.Environment]::GetEnvironmentVariable("FIREBASE_PROJECT_ID", "User")
$credentialsPath = [System.Environment]::GetEnvironmentVariable("FIREBASE_CREDENTIALS_PATH", "User")

if ($projectId) {
    Write-Host "[OK] FIREBASE_PROJECT_ID: $projectId" -ForegroundColor Green
} else {
    Write-Host "[ERROR] FIREBASE_PROJECT_ID no configurado" -ForegroundColor Red
    Write-Host "Configura con: [System.Environment]::SetEnvironmentVariable('FIREBASE_PROJECT_ID', 'poolpromanager-e3ff5', 'User')" -ForegroundColor Yellow
    exit 1
}

if ($credentialsPath -and (Test-Path $credentialsPath)) {
    Write-Host "[OK] Credenciales Firebase: $credentialsPath" -ForegroundColor Green
} else {
    Write-Host "[ERROR] Credenciales Firebase no encontradas" -ForegroundColor Red
    Write-Host "Verifica que el archivo serviceAccountKey.json exista en backend/" -ForegroundColor Yellow
    exit 1
}

# Verificar PostgreSQL
Write-Host ""
Write-Host "Verificando PostgreSQL..." -ForegroundColor Yellow
$postgresRunning = docker ps --filter "name=poolpro-postgres" --format "{{.Names}}" 2>$null

if ($postgresRunning -eq "poolpro-postgres") {
    Write-Host "[OK] PostgreSQL está corriendo" -ForegroundColor Green
} else {
    Write-Host "[ADVERTENCIA] PostgreSQL no está corriendo" -ForegroundColor Yellow
    Write-Host "Intentando iniciar PostgreSQL..." -ForegroundColor Yellow
    
    docker start poolpro-postgres 2>$null
    Start-Sleep -Seconds 3
    
    $postgresRunning = docker ps --filter "name=poolpro-postgres" --format "{{.Names}}" 2>$null
    if ($postgresRunning -eq "poolpro-postgres") {
        Write-Host "[OK] PostgreSQL iniciado" -ForegroundColor Green
    } else {
        Write-Host "[ERROR] No se pudo iniciar PostgreSQL" -ForegroundColor Red
        Write-Host "Asegúrate de que Docker Desktop esté corriendo" -ForegroundColor Yellow
        Write-Host "O crea el contenedor con: docker run -d --name poolpro-postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=poolpro -p 5432:5432 postgres:15" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "Iniciando backend..." -ForegroundColor Yellow
Write-Host "Busca en los logs: 'Firebase inicializado correctamente para proyecto: poolpromanager-e3ff5'" -ForegroundColor Cyan
Write-Host ""

# Cambiar al directorio backend
Set-Location $PSScriptRoot

# Iniciar backend
.\gradlew.bat clean bootRun


