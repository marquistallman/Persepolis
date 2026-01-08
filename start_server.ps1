# Script de gestión para Servidor Persepolis (Windows)

$RepoUrl = "https://github.com/marquistallman/Persepolis.git"

Write-Host "--- Gestor de Servidor Persepolis ---" -ForegroundColor Cyan

# Asegurar que git está inicializado
if (-not (Test-Path ".git")) {
    Write-Host "Inicializando repositorio..."
    git init
    git remote add origin $RepoUrl
}

Write-Host "`nMenú de Opciones:"
Write-Host "1. Recibir cambios de 'developer' (Actualizar)"
Write-Host "2. Instalar Sistema (LMMfunction y data)"
Write-Host "3. Salir"
$Option = Read-Host "Seleccione una opción"

if ($Option -eq "1") {
    Write-Host "Conectando con rama 'developer'..."
    git fetch origin developer
    if ($LASTEXITCODE -ne 0) { Write-Host "Error: No se pudo conectar con el repositorio."; exit }
    
    Write-Host "Recibiendo cambios..."
    git checkout developer
    git pull origin developer
}
elseif ($Option -eq "2") {
    Write-Host "Configurando instalación del sistema..."
    
    # Configurar Sparse Checkout
    git config core.sparseCheckout true
    $SparseFile = ".git/info/sparse-checkout"
    
    if (-not (Test-Path ".git/info")) { New-Item -ItemType Directory -Path ".git/info" -Force | Out-Null }
    
    Set-Content -Path $SparseFile -Value "LMMfunction/"
    Add-Content -Path $SparseFile -Value "data/"
    Add-Content -Path $SparseFile -Value "start_server.ps1"
    Add-Content -Path $SparseFile -Value "start_server.sh"
    Add-Content -Path $SparseFile -Value "mount_server.ps1"
    Add-Content -Path $SparseFile -Value "mount_server.sh"
    Add-Content -Path $SparseFile -Value "README.md"
    
    Write-Host "Descargando archivos del sistema..."
    git fetch origin developer
    if ($LASTEXITCODE -ne 0) { Write-Host "Error: No se pudo conectar con el repositorio."; exit }
    
    # Forzar estado exacto de la rama
    git checkout -f -B developer origin/developer
    
    Write-Host "Limpiando archivos no necesarios..."
    git clean -fdx
}
elseif ($Option -eq "3") {
    exit
}
else {
    Write-Host "Opción no válida."
}

if ($Option -eq "1" -or $Option -eq "2") {
    $Continue = Read-Host "`n¿Desea continuar ejecutando 'mount_server.ps1'? (S/N)"
    if ($Continue -eq "S" -or $Continue -eq "s") {
        if (Test-Path "mount_server.ps1") {
            & .\mount_server.ps1
        } else {
            Write-Host "Error: mount_server.ps1 no encontrado." -ForegroundColor Red
        }
    }
}

Write-Host "--- Proceso Finalizado ---" -ForegroundColor Green
