#!/bin/bash
# Script de configuración interactivo para Persepolis (Linux/Mac)
git config core.filemode false
REPO_URL="https://github.com/marquistallman/Persepolis.git"

echo "--- Configuración de Entorno Persepolis ---"

# 1. Preguntar Modo
echo -e "\nSelecciona el modo de instalación:"
echo "1. Servidor (Producción) -> Rama 'server' (Descarga ligera)"
echo "2. Desarrollo (Local)    -> Rama 'developer' (Descarga completa)"
read -p "Opción (1 o 2): " MODE

# 3. Inicializar Git
if [ ! -d ".git" ]; then
    echo "Inicializando repositorio Git..."
    git init
    git remote add origin "$REPO_URL"
else
    echo "Repositorio Git detectado. Asegurando URL del origen..."
    git remote set-url origin "$REPO_URL"
fi

# 4. Configurar según la elección
# Desactivar sparse checkout
git config core.sparseCheckout false

if [ "$MODE" == "1" ]; then
    BRANCH="server"
    echo "Modo SERVIDOR seleccionado. Preparando rama 'server'..."
else
    BRANCH="developer"
    echo "Modo DESARROLLO seleccionado. Preparando rama 'developer'..."
fi

# 5. Descargar contenido
echo "Sincronizando con rama '$BRANCH'..."
git pull origin "$BRANCH"
git checkout "$BRANCH"

# Limpieza extra para Modo Servidor
if [ "$MODE" == "1" ]; then
    echo "Limpiando archivos residuales..."
    git clean -fdx
fi

echo "--- Operación Completada ---"
echo "Eliminando instaladores y documentación..."
rm -f setup_server.ps1 .gitignore README.md setup_server.sh
