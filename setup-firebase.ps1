# Script para configurar Firebase en el backend
# Este script te guía paso a paso para configurar Firebase

Write-Host "=== Configuracion de Firebase para el Backend ===" -ForegroundColor Cyan
Write-Host ""

Write-Host "Este script te ayudara a configurar Firebase." -ForegroundColor Yellow
Write-Host ""

# Paso 1: Verificar si ya existe configuración
Write-Host "Paso 1: Verificando configuracion actual..." -ForegroundColor Yellow
$projectId = $env:FIREBASE_PROJECT_ID
$credentialsPath = $env:FIREBASE_CREDENTIALS_PATH
$credentialsJson = $env:FIREBASE_CREDENTIALS_JSON

if ($projectId) {
    Write-Host "  Project ID encontrado: $projectId" -ForegroundColor Green
} else {
    Write-Host "  Project ID no configurado" -ForegroundColor Yellow
}

if ($credentialsPath) {
    Write-Host "  Ruta de credenciales encontrada: $credentialsPath" -ForegroundColor Green
    if (Test-Path $credentialsPath) {
        Write-Host "  Archivo de credenciales existe" -ForegroundColor Green
    } else {
        Write-Host "  Archivo de credenciales NO existe en la ruta especificada" -ForegroundColor Red
    }
} elseif ($credentialsJson) {
    Write-Host "  Credenciales configuradas como variable de entorno" -ForegroundColor Green
} else {
    Write-Host "  Credenciales no configuradas" -ForegroundColor Yellow
}

Write-Host ""

# Instrucciones
Write-Host "Paso 2: Sigue estos pasos para configurar Firebase:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. Ve a https://console.firebase.google.com/" -ForegroundColor Cyan
Write-Host "2. Crea un proyecto o selecciona uno existente" -ForegroundColor Cyan
Write-Host "3. Ve a Configuracion del proyecto > Cuentas de servicio" -ForegroundColor Cyan
Write-Host "4. Genera una nueva clave privada (se descargara un archivo JSON)" -ForegroundColor Cyan
Write-Host "5. Guarda el archivo JSON en una ubicacion segura" -ForegroundColor Cyan
Write-Host ""

# Solicitar información
Write-Host "Paso 3: Configurar variables de entorno" -ForegroundColor Yellow
Write-Host ""

$projectIdInput = Read-Host "Ingresa el Firebase Project ID (o presiona Enter para omitir)"
if ($projectIdInput) {
    $env:FIREBASE_PROJECT_ID = $projectIdInput
    Write-Host "  Project ID configurado: $projectIdInput" -ForegroundColor Green
}

Write-Host ""
Write-Host "Opcion A: Usar archivo de credenciales" -ForegroundColor Cyan
$useFile = Read-Host "Deseas usar un archivo de credenciales? (S/N)"
if ($useFile -eq "S" -or $useFile -eq "s") {
    $filePath = Read-Host "Ingresa la ruta completa al archivo JSON de credenciales"
    if (Test-Path $filePath) {
        $env:FIREBASE_CREDENTIALS_PATH = $filePath
        Write-Host "  Ruta de credenciales configurada" -ForegroundColor Green
    } else {
        Write-Host "  Error: El archivo no existe en la ruta especificada" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "=== Resumen de Configuracion ===" -ForegroundColor Cyan
Write-Host "Project ID: $env:FIREBASE_PROJECT_ID" -ForegroundColor Gray
Write-Host "Credentials Path: $env:FIREBASE_CREDENTIALS_PATH" -ForegroundColor Gray
Write-Host ""

if ($env:FIREBASE_PROJECT_ID) {
    Write-Host "Para hacer permanente esta configuracion:" -ForegroundColor Yellow
    Write-Host "1. Agrega estas variables a tu perfil de PowerShell:" -ForegroundColor Gray
    Write-Host '   [System.Environment]::SetEnvironmentVariable("FIREBASE_PROJECT_ID", "' + $env:FIREBASE_PROJECT_ID + '", "User")' -ForegroundColor Gray
    if ($env:FIREBASE_CREDENTIALS_PATH) {
        Write-Host '   [System.Environment]::SetEnvironmentVariable("FIREBASE_CREDENTIALS_PATH", "' + $env:FIREBASE_CREDENTIALS_PATH + '", "User")' -ForegroundColor Gray
    }
    Write-Host ""
    Write-Host "2. O crea un archivo .env en la carpeta backend con:" -ForegroundColor Gray
    Write-Host "   FIREBASE_PROJECT_ID=$env:FIREBASE_PROJECT_ID" -ForegroundColor Gray
    if ($env:FIREBASE_CREDENTIALS_PATH) {
        Write-Host "   FIREBASE_CREDENTIALS_PATH=$env:FIREBASE_CREDENTIALS_PATH" -ForegroundColor Gray
    }
}

Write-Host ""
Write-Host "Ahora puedes reiniciar el backend para que use Firebase." -ForegroundColor Green



