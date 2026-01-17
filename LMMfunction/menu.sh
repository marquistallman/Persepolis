#!/bin/bash

# Asegurar que el script se ejecuta en su propia carpeta
cd "$(dirname "$0")" || exit

# --- CONFIGURACIÓN DE BASE DE DATOS ---
# Define aquí las credenciales para que Maven pueda conectar a la BD durante los tests
export DB_USER="persepolis_user"
export DB_PASSWORD="contrasena"

# Variables de color
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

function show_header {
    clear
    echo -e "${CYAN}==========================================${NC}"
    echo -e "${CYAN}   WALLPAPER SYSTEM - PANEL DE CONTROL    ${NC}"
    echo -e "${CYAN}==========================================${NC}"
    echo ""
}

function pause_script {
    echo ""
    read -p "Presiona Enter para continuar..."
}

function check_command {
    if ! command -v "$1" &> /dev/null; then
        echo -e "${RED}❌ Error: No tienes instalado $2 ($1).${NC}"
        return 1
    fi
    return 0
}

function run_server {
    check_command "mvn" "Maven" || { pause_script; return; }
    echo -e "${GREEN}--> Iniciando Spring Boot...${NC}"
    mvn spring-boot:run
    pause_script
}

function sync_git {
    check_command "git" "Git" || { pause_script; return; }
    
    # Ir a la raíz del repo
    REPO_ROOT=$(git rev-parse --show-toplevel)
    if [ -n "$REPO_ROOT" ]; then
        pushd "$REPO_ROOT" > /dev/null || return
    fi

    echo -e "${YELLOW}--> Comprobando configuración de Git...${NC}"
    if [ -z "$(git config user.name)" ]; then
        echo -e "${CYAN}👋 ¡Hola! Parece que es tu primera vez.${NC}"
        read -p "--> Escribe tu Nombre: " git_name
        read -p "--> Escribe tu Email: " git_email
        git config --global user.name "$git_name"
        git config --global user.email "$git_email"
        git config --global credential.helper store
        echo -e "${GREEN}✅ Configuración guardada.${NC}"
    fi

    echo -e "${YELLOW}--> 1. Descargando cambios (git pull)...${NC}"
    git pull
    if [ $? -ne 0 ]; then
        echo -e "${RED}❌ Error al descargar. Hay conflictos que debes resolver manualmente.${NC}"
        if [ -n "$REPO_ROOT" ]; then popd > /dev/null; fi
        pause_script
        return
    fi

    echo -e "\n${CYAN}--> Estado de tus archivos:${NC}"
    git status -s

    echo ""
    read -p "¿Quieres subir estos cambios a la nube? (s/n): " confirm
    if [[ "$confirm" =~ ^[sS]$ ]]; then
        echo -e "${YELLOW}--> 2. Añadiendo archivos...${NC}"
        git add .
        
        read -p "--> Mensaje del commit (Enter para automático): " msg
        if [ -z "$msg" ]; then msg="Actualización automática del equipo"; fi
        
        git commit -m "$msg"
        
        echo -e "${YELLOW}--> 3. Subiendo a GitHub...${NC}"
        git push
        echo -e "${GREEN}✅ ¡Sincronización completada!${NC}"
    else
        echo "Operación cancelada. Solo se descargaron cambios."
    fi
    
    if [ -n "$REPO_ROOT" ]; then popd > /dev/null; fi
    pause_script
}

function integrate_frontend {
    check_command "npm" "Node.js" || { pause_script; return; }

    SOURCE="../front/Front"
    DEST="src/main/resources/static"

    if [ ! -d "$SOURCE" ]; then
        echo -e "${RED}❌ Error: No encuentro la carpeta '../front/Front'.${NC}"
        pause_script
        return
    fi

    echo -e "${YELLOW}--> Compilando Tailwind CSS...${NC}"
    pushd "$SOURCE" > /dev/null || return
    if [ ! -d "node_modules" ]; then
        echo "    (Instalando dependencias...)"
        npm install
    fi
    npm run build
    if [ $? -ne 0 ]; then
        echo -e "${RED}❌ Falló la compilación de Tailwind.${NC}"
        popd > /dev/null
        pause_script
        return
    fi
    popd > /dev/null

    echo -e "${YELLOW}--> Copiando archivos a static (filtrando)...${NC}"
    mkdir -p "$DEST"
    rm -rf "${DEST:?}"/*
    
    # Copiar excluyendo node_modules y archivos de configuración
    if command -v rsync &> /dev/null; then
        rsync -av --exclude='node_modules' --exclude='.git' --exclude='package.json' --exclude='package-lock.json' --exclude='tailwind.config.js' --exclude='.gitignore' "$SOURCE/" "$DEST/"
    else
        for file in "$SOURCE"/*; do
            name=$(basename "$file")
            case "$name" in
                node_modules|.git|package.json|package-lock.json|tailwind.config.js|.gitignore) ;;
                *) cp -r "$file" "$DEST/" ;;
            esac
        done
    fi

    echo -e "${GREEN}✅ Diseño integrado correctamente.${NC}"
    pause_script
}

function build_jar {
    check_command "mvn" "Maven" || { pause_script; return; }
    echo -e "${YELLOW}--> Generando archivo .jar...${NC}"
    mvn clean package
    echo -e "${GREEN}✅ Archivo generado en la carpeta 'target'.${NC}"
    pause_script
}

function fix_large_files {
    echo -e "${CYAN}--> 🚑 Reparando error de archivo .jar gigante...${NC}"
    echo -e "${YELLOW}--> ⚠️  Esta opción REINICIA tus commits locales para limpiar el historial.${NC}"
    
    REPO_ROOT=$(git rev-parse --show-toplevel)
    if [ -z "$REPO_ROOT" ]; then
        echo -e "${RED}❌ No se encontró la raíz del repositorio.${NC}"
        pause_script
        return
    fi
    pushd "$REPO_ROOT" > /dev/null || return

    echo -e "${YELLOW}--> Reseteando historial local (git reset --mixed)...${NC}"
    git fetch origin
    git reset --mixed origin/master
    
    echo -e "${YELLOW}--> Re-agregando archivos limpios...${NC}"
    git add .
    
    echo -e "${YELLOW}--> Creando nuevo commit limpio...${NC}"
    git commit -m "Corrección automática: Archivos grandes eliminados"
    
    echo -e "${YELLOW}--> Subiendo a GitHub...${NC}"
    git push
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ ¡Arreglado! Historial limpio y subido.${NC}"
    else
        echo -e "${RED}❌ Error al subir. Revisa la consola.${NC}"
    fi
    
    popd > /dev/null
    pause_script
}

# --- Funciones Específicas para Frontend Dev ---

function frontend_install {
    check_command "npm" "Node.js" || { pause_script; return; }
    SOURCE="../front/Front"
    if [ ! -d "$SOURCE" ]; then
        echo -e "${RED}❌ Error: No encuentro la carpeta '../front/Front'.${NC}"
        pause_script
        return
    fi
    echo -e "${YELLOW}--> Instalando dependencias en $SOURCE...${NC}"
    pushd "$SOURCE" > /dev/null || return
    npm install
    popd > /dev/null
    echo -e "${GREEN}✅ Dependencias instaladas.${NC}"
    pause_script
}

function frontend_dev {
    check_command "npm" "Node.js" || { pause_script; return; }
    SOURCE="../front/Front"
    if [ ! -d "$SOURCE" ]; then
        echo -e "${RED}❌ Error: No encuentro la carpeta '../front/Front'.${NC}"
        pause_script
        return
    fi
    echo -e "${YELLOW}--> Iniciando modo desarrollo en $SOURCE...${NC}"
    echo -e "${CYAN}(Presiona Ctrl+C para detener)${NC}"
    pushd "$SOURCE" > /dev/null || return
    npm run dev
    popd > /dev/null
    pause_script
}

function open_browser {
    URL="http://localhost:8080"
    echo -e "${YELLOW}--> Abriendo $URL...${NC}"
    if [[ "$OSTYPE" == "darwin"* ]]; then
        open "$URL"
    elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
        xdg-open "$URL"
    else
        echo -e "${RED}Sistema operativo no detectado para abrir navegador.${NC}"
    fi
    pause_script
}

function show_location {
    echo -e "${CYAN}--> Ubicación actual:${NC}"
    pwd
    echo -e "${CYAN}--> Contenido:${NC}"
    ls -F
    pause_script
}

# Main Loop
while true; do
    show_header
    echo "1. 🚀 Iniciar Servidor (Backend)"
    echo "2. ☁️  Sincronizar con GitHub (Guardar/Bajar cambios)"
    echo "3. 🎨 Integrar Frontend (Traer diseño nuevo)"
    echo "4. 📦 Generar Ejecutable (.jar)"
    echo "5. 🚑 Reparar error de subida (Archivos grandes)"
    echo "------------------------------------------"
    echo "6. 📦 Frontend: Instalar Dependencias (npm install)"
    echo "7. 🛠️  Frontend: Modo Desarrollo (npm run dev)"
    echo "8. 🌐 Abrir Navegador (localhost:8080)"
    echo "9. 📍 Ver ubicación actual"
    echo "0. Salir"
    echo ""
    
    read -p "Selecciona una opción: " selection
    
    case $selection in
        1) run_server ;;
        2) sync_git ;;
        3) integrate_frontend ;;
        4) build_jar ;;
        5) fix_large_files ;;
        6) frontend_install ;;
        7) frontend_dev ;;
        8) open_browser ;;
        9) show_location ;;
        0) exit 0 ;;
        *) echo -e "${RED}Opción no válida.${NC}"; sleep 1 ;;
    esac
done
