# Script de configuración interactivo para Persepolis (Windows)

$RepoUrl = "https://github.com/marquistallman/Persepolis.git"

Write-Host "--- Configuración de Entorno Persepolis ---" -ForegroundColor Cyan

# 1. Preguntar Modo
Write-Host "`nSelecciona el modo de instalación:"
Write-Host "1. Servidor (Producción) -> Rama 'server' (Descarga ligera)"
Write-Host "2. Desarrollo (Local)    -> Rama 'developer' (Descarga completa)"
$Mode = Read-Host "Opción (1 o 2)"

# 3. Inicializar Git
if (-not (Test-Path ".git")) {
    Write-Host "Inicializando repositorio Git..."
    git init
    git remote add origin $RepoUrl
} else {
    Write-Host "Repositorio Git detectado. Asegurando URL del origen..."
    git remote set-url origin $RepoUrl
}

# 4. Configurar según la elección
# Desactivar sparse checkout para evitar conflictos y bajar la rama completa
git config core.sparseCheckout false

if ($Mode -eq "1") {
    $Branch = "server"
    Write-Host "Modo SERVIDOR seleccionado. Preparando rama 'server'..."
} else {
    $Branch = "developer"
    Write-Host "Modo DESARROLLO seleccionado. Preparando rama 'developer'..."
}

# 5. Descargar contenido
Write-Host "Sincronizando con rama '$Branch'..."
git pull origin $Branch
git checkout $Branch

# Limpieza extra para Modo Servidor: Borrar archivos no rastreados (basura)
if ($Mode -eq "1") {
    Write-Host "Limpiando archivos residuales..."
    # Limpieza profunda de Git (x minúscula = borra todo lo no rastreado)
    git clean -fdx

    Write-Host "Eliminando instaladores y documentación..."
    Remove-Item -Path "setup_server.sh", ".gitignore", "README.md", "setup_server.ps1" -Force -ErrorAction SilentlyContinue
}

Write-Host "--- Operación Completada ---" -ForegroundColor Green