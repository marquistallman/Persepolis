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

    Write-Host "--> Copiando archivos a static (filtrando)..." -ForegroundColor Yellow
    if (-not (Test-Path $Dest)) { New-Item -ItemType Directory -Path $Dest | Out-Null }
    
    # Limpiar destino
    Get-ChildItem -Path $Dest -Recurse | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
    
    # Copiar filtrando desde el origen (mucho más rápido)
    $Exclusions = @("node_modules", ".git", "package.json", "package-lock.json", "tailwind.config.js", ".gitignore")
    Get-ChildItem -Path $Source | Where-Object { $_.Name -notin $Exclusions } | Copy-Item -Destination $Dest -Recurse -Force

    Write-Host "✅ Diseño integrado correctamente." -ForegroundColor Green
    Pause-Script
}

function Build-Jar {
    if (-not (Check-Command "mvn" "Maven")) { Pause-Script; return }
    
    # Verificar si el frontend está integrado antes de compilar para evitar JAR vacío
    if (-not (Test-Path "src/main/resources/static/Pages")) {
        Write-Host "⚠️  ALERTA: No se detectaron archivos de frontend en 'static/Pages'." -ForegroundColor Yellow
        $ans = Read-Host "    ¿Quieres integrar el frontend antes de generar el JAR? (s/n)"
        if ($ans -match "^[sS]") { Integrate-Frontend }
    }

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

# --- Funciones Específicas para Frontend Dev ---

function Frontend-Install {
    if (-not (Check-Command "npm" "Node.js")) { Pause-Script; return }
    $Source = "../front/Front"
    if (-not (Test-Path $Source)) {
        Write-Host "❌ Error: No encuentro la carpeta '../front/Front'." -ForegroundColor Red
        Pause-Script
        return
    }
    Write-Host "--> Instalando dependencias en $Source..." -ForegroundColor Yellow
    Push-Location $Source
    npm install
    Pop-Location
    Write-Host "✅ Dependencias instaladas." -ForegroundColor Green
    Pause-Script
}

function Frontend-Dev {
    if (-not (Check-Command "npm" "Node.js")) { Pause-Script; return }
    $Source = "../front/Front"
    if (-not (Test-Path $Source)) {
        Write-Host "❌ Error: No encuentro la carpeta '../front/Front'." -ForegroundColor Red
        Pause-Script
        return
    }
    Write-Host "--> Iniciando modo desarrollo en $Source..." -ForegroundColor Yellow
    Write-Host "(Presiona Ctrl+C para detener)" -ForegroundColor Cyan
    Push-Location $Source
    npm run dev
    Pop-Location
    Pause-Script
}

function Open-Browser {
    $URL = "http://localhost:8080"
    Write-Host "--> Abriendo $URL..." -ForegroundColor Yellow
    Start-Process $URL
    Pause-Script
}

function Show-Location {
    Write-Host "--> Ubicación actual:" -ForegroundColor Cyan
    Get-Location
    Write-Host "--> Contenido:" -ForegroundColor Cyan
    Get-ChildItem -Name
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
    Write-Host "------------------------------------------"
    Write-Host "6. 📦 Frontend: Instalar Dependencias (npm install)"
    Write-Host "7. 🛠️  Frontend: Modo Desarrollo (npm run dev)"
    Write-Host "8. 🌐 Abrir Navegador (localhost:8080)"
    Write-Host "9. 📍 Ver ubicación actual"
    Write-Host "0. Salir"
    Write-Host ""
    
    $selection = Read-Host "Selecciona una opción"
    
    switch ($selection) {
        '1' { Run-Server }
        '2' { Sync-Git }
        '3' { Integrate-Frontend }
        '4' { Build-Jar }
        '5' { Fix-LargeFiles }
        '6' { Frontend-Install }
        '7' { Frontend-Dev }
        '8' { Open-Browser }
        '9' { Show-Location }
        '0' { exit }
        default { Write-Host "Opción no válida." -ForegroundColor Red; Start-Sleep -Seconds 1 }
    }
} while ($true)
