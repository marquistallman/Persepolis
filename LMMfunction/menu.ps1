<#
.SYNOPSIS
    Script de menú interactivo para el equipo de WallPaperSystem.
#>

# Asegurar que el script se ejecuta en su propia carpeta (LMMfunction) para evitar errores de rutas
Set-Location $PSScriptRoot

function Show-Header {
    Clear-Host
    Write-Host "==========================================" -ForegroundColor Cyan
    Write-Host "   WALLPAPER SYSTEM - PANEL DE CONTROL    " -ForegroundColor Cyan
    Write-Host "==========================================" -ForegroundColor Cyan
    Write-Host ""
}

function Pause-Script {
    Write-Host ""
    Read-Host "Presiona Enter para continuar..."
}

function Check-Command ($cmd, $name) {
    if (-not (Get-Command $cmd -ErrorAction SilentlyContinue)) {
        Write-Host "❌ Error: No tienes instalado $name ($cmd)." -ForegroundColor Red
        return $false
    }
    return $true
}

function Run-Server {
    if (-not (Check-Command "mvn" "Maven")) { Pause-Script; return }
    Write-Host "--> Iniciando Spring Boot..." -ForegroundColor Green
    mvn spring-boot:run
    Pause-Script
}

function Sync-Git {
    if (-not (Check-Command "git" "Git")) { Pause-Script; return }
    
    # Ir a la raíz del repositorio para sincronizar TODO (backend + frontend), no solo la carpeta actual
    $repoRoot = git rev-parse --show-toplevel
    if ($repoRoot) { Push-Location $repoRoot }

    Write-Host "--> Comprobando configuración de Git..." -ForegroundColor Yellow
    $userName = git config user.name
    if (-not $userName) {
        Write-Host "👋 ¡Hola! Parece que es tu primera vez." -ForegroundColor Magenta
        $name = Read-Host "--> Escribe tu Nombre"
        $email = Read-Host "--> Escribe tu Email"
        git config --global user.name "$name"
        git config --global user.email "$email"
        git config --global credential.helper store
        Write-Host "✅ Configuración guardada." -ForegroundColor Green
    }

    Write-Host "--> 1. Descargando cambios (git pull)..." -ForegroundColor Yellow
    git pull
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Error al descargar. Hay conflictos que debes resolver manualmente." -ForegroundColor Red
        Pause-Script
        if ($repoRoot) { Pop-Location }
        return
    }

    Write-Host "`n--> Estado de tus archivos:" -ForegroundColor Cyan
    git status -s

    Write-Host ""
    $confirm = Read-Host "¿Quieres subir estos cambios a la nube? (s/n)"
    if ($confirm -match "^[sS]") {
        Write-Host "--> 2. Añadiendo archivos..." -ForegroundColor Yellow
        git add .
        
        $msg = Read-Host "--> Mensaje del commit (Enter para automático)"
        if (-not $msg) { $msg = "Actualización automática del equipo" }
        
        git commit -m "$msg"
        
        Write-Host "--> 3. Subiendo a GitHub..." -ForegroundColor Yellow
        git push
        Write-Host "✅ ¡Sincronización completada!" -ForegroundColor Green
    } else {
        Write-Host "Operación cancelada. Solo se descargaron cambios." -ForegroundColor Gray
    }
    if ($repoRoot) { Pop-Location }
    Pause-Script
}

function Integrate-Frontend {
    if (-not (Check-Command "npm" "Node.js")) { Pause-Script; return }

    $Source = "../front/Front"
    $Dest = "src/main/resources/static"

    if (-not (Test-Path $Source)) {
        Write-Host "❌ Error: No encuentro la carpeta '../front/Front'." -ForegroundColor Red
        Pause-Script
        return
    }

    Write-Host "--> Compilando Tailwind CSS..." -ForegroundColor Yellow
    Push-Location $Source
    try {
        if (-not (Test-Path "node_modules")) { 
            Write-Host "    (Instalando dependencias...)"
            npm install 
        }
        npm run build
        if ($LASTEXITCODE -ne 0) { throw "Error en build" }
    }
    catch {
        Write-Host "❌ Falló la compilación de Tailwind." -ForegroundColor Red
        Pop-Location
        Pause-Script
        return
    }
    Pop-Location

    Write-Host "--> Copiando archivos a static..." -ForegroundColor Yellow
    if (-not (Test-Path $Dest)) { New-Item -ItemType Directory -Path $Dest | Out-Null }
    
    # Limpiar destino y copiar
    Get-ChildItem -Path $Dest -Recurse | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
    Copy-Item -Path "$Source\*" -Destination $Dest -Recurse -Force

    # Limpiar basura de desarrollo
    $Exclusions = @("node_modules", ".git", "package.json", "package-lock.json", "tailwind.config.js", ".gitignore")
    foreach ($item in $Exclusions) {
        $path = Join-Path $Dest $item
        if (Test-Path $path) { Remove-Item $path -Recurse -Force -ErrorAction SilentlyContinue }
    }

    Write-Host "✅ Diseño integrado correctamente." -ForegroundColor Green
    Pause-Script
}

function Build-Jar {
    if (-not (Check-Command "mvn" "Maven")) { Pause-Script; return }
    Write-Host "--> Generando archivo .jar..." -ForegroundColor Yellow
    mvn clean package
    Write-Host "✅ Archivo generado en la carpeta 'target'." -ForegroundColor Green
    Pause-Script
}

function Fix-LargeFiles {
    Write-Host "--> 🚑 Reparando error de archivo .jar gigante..." -ForegroundColor Magenta
    Write-Host "--> ⚠️  Esta opción REINICIA tus commits locales para limpiar el historial." -ForegroundColor Yellow
    Write-Host "    (Tus archivos NO se borran, solo se vuelven a preparar para subir sin el archivo pesado)"
    
    # 1. Ir a la raíz del repositorio para asegurar que operamos sobre todo
    $repoRoot = git rev-parse --show-toplevel
    if (-not $repoRoot) {
        Write-Host "❌ No se encontró la raíz del repositorio." -ForegroundColor Red
        Pause-Script
        return
    }
    Push-Location $repoRoot

    # 2. Reset MIXED: Mueve el puntero atrás y saca todo del 'staging', pero deja los archivos en disco
    Write-Host "--> Reseteando historial local (git reset --mixed)..." -ForegroundColor Yellow
    git fetch origin
    git reset --mixed origin/master
    
    # 3. Volver a agregar todo (ahora respetará el .gitignore y ignorará 'target')
    Write-Host "--> Re-agregando archivos limpios..." -ForegroundColor Yellow
    git add .
    
    # 4. Crear un nuevo commit y subir
    Write-Host "--> Creando nuevo commit limpio..." -ForegroundColor Yellow
    git commit -m "Corrección automática: Archivos grandes eliminados"
    
    Write-Host "--> Subiendo a GitHub..." -ForegroundColor Yellow
    git push
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ ¡Arreglado! Historial limpio y subido." -ForegroundColor Green
    } else {
        Write-Host "❌ Error al subir. Revisa la consola." -ForegroundColor Red
    }
    
    Pop-Location
    Pause-Script
}

# Bucle Principal
do {
    Show-Header
    Write-Host "1. 🚀 Iniciar Servidor (Backend)"
    Write-Host "2. ☁️  Sincronizar con GitHub (Guardar/Bajar cambios)"
    Write-Host "3. 🎨 Integrar Frontend (Traer diseño nuevo)"
    Write-Host "4. 📦 Generar Ejecutable (.jar)"
    Write-Host "5. 🚑 Reparar error de subida (Archivos grandes)"
    Write-Host "0. Salir"
    Write-Host ""
    
    $selection = Read-Host "Selecciona una opción"
    
    switch ($selection) {
        '1' { Run-Server }
        '2' { Sync-Git }
        '3' { Integrate-Frontend }
        '4' { Build-Jar }
        '5' { Fix-LargeFiles }
        '0' { exit }
        default { Write-Host "Opción no válida." -ForegroundColor Red; Start-Sleep -Seconds 1 }
    }
} while ($true)
