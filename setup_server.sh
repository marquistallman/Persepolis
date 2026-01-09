#!/bin/bash
# Script de configuración interactivo para Persepolis (Linux/Mac)

set -e  # cortar si algo falla

REPO_URL="https://github.com/marquistallman/Persepolis.git"

echo "--- Configuración de Entorno Persepolis ---"

# 1. Preguntar Modo
echo -e "\nSelecciona el modo de instalación:"
echo "1. Servidor (Producción) -> Rama 'server'"
echo "2. Desarrollo (Local)    -> Rama 'developer'"
read -p "Opción (1 o 2): " MODE

if [ "$MODE" == "1" ]; then
    BRANCH="server"
    echo "Modo SERVIDOR seleccionado."
else
    BRANCH="developer"
    echo "Modo DESARROLLO seleccionado."
fi

# 2. Inicializar Git
if [ ! -d ".git" ]; then
    echo "Inicializando repositorio Git..."
    git init
    git remote add origin "$REPO_URL"
else
    echo "Repositorio Git detectado. Actualizando origen..."
    git remote set-url origin "$REPO_URL"
fi

# 3. Asegurar configuración limpia
git config core.sparseCheckout false
git fetch origin

# 4. Crear / forzar rama local desde remoto (CLAVE)
echo "Sincronizando rama '$BRANCH'..."
git checkout -B "$BRANCH" "origin/$BRANCH"

# 5. Limpieza extra para servidor
if [ "$MODE" == "1" ]; then
    echo "Limpieza profunda para entorno servidor..."
    git clean -fdx
fi

# 6. Limpieza final de instaladores
echo "Eliminando archivos innecesarios..."
rm -f setup_server.ps1 setup_server.sh README.md

echo "--- Operación completada con éxito ---"
