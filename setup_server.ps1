# Script de configuración interactivo para Persepolis (Windows)

$RepoUrl = "https://github.com/marquistallman/Persepolis.git"

Write-Host "--- Configuración de Entorno Persepolis ---" -ForegroundColor Cyan

# 1. Preguntar Rama
$BranchInput = Read-Host "Introduce la rama a utilizar (Presiona Enter para 'dev')"
$Branch = if ([string]::IsNullOrWhiteSpace($BranchInput)) { "dev" } else { $BranchInput }

# 2. Preguntar Modo
Write-Host "`nSelecciona el modo de instalación:"
Write-Host "1. Servidor (Producción) -> Descarga ligera (Solo LMMfunction y data)"
Write-Host "2. Desarrollo (Local)    -> Descarga completa (Todo el repositorio)"
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
if ($Mode -eq "1") {
    Write-Host "Modo SERVIDOR seleccionado. Activando Sparse Checkout..."
    git config core.sparseCheckout true
    
    $SparseFile = ".git/info/sparse-checkout"
    Write-Host "Configurando filtros..."
    Set-Content -Path $SparseFile -Value "LMMfunction/"
    Add-Content -Path $SparseFile -Value "data/"
    Add-Content -Path $SparseFile -Value "setup_server.ps1"
    Add-Content -Path $SparseFile -Value "setup_server.sh"
    Add-Content -Path $SparseFile -Value "README.md"
} else {
    Write-Host "Modo DESARROLLO seleccionado. Descarga completa..."
    git config core.sparseCheckout false
    # Si existía configuración previa de sparse, esto asegura que se baje todo
}

# 5. Descargar contenido
Write-Host "Sincronizando con rama '$Branch'..."
git pull origin $Branch
if ($Mode -ne "1") { git checkout $Branch } # Asegurar checkout en modo dev

# Limpieza extra para Modo Servidor: Borrar archivos no rastreados (basura)
if ($Mode -eq "1") {
    Write-Host "Limpiando archivos residuales..."
    git clean -fdX
}

Write-Host "--- Operación Completada ---" -ForegroundColor Green