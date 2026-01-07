#!/bin/bash
# Script de configuración interactivo para Persepolis (Linux/Mac)

REPO_URL="https://github.com/marquistallman/Persepolis.git"

echo "--- Configuración de Entorno Persepolis ---"

# 1. Preguntar Rama
read -p "Introduce la rama a utilizar (Enter para 'dev'): " BRANCH_INPUT
BRANCH=${BRANCH_INPUT:-dev}

# 2. Preguntar Modo
echo -e "\nSelecciona el modo de instalación:"
echo "1. Servidor (Producción) -> Descarga ligera (Solo LMMfunction y data)"
echo "2. Desarrollo (Local)    -> Descarga completa (Todo el repositorio)"
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
if [ "$MODE" == "1" ]; then
    echo "Modo SERVIDOR seleccionado. Activando Sparse Checkout..."
    git config core.sparseCheckout true
    
    echo "Configurando filtros..."
    echo "LMMfunction/" > .git/info/sparse-checkout
    echo "data/" >> .git/info/sparse-checkout
    echo "setup_server.sh" >> .git/info/sparse-checkout
    echo "setup_server.ps1" >> .git/info/sparse-checkout
    echo "README.md" >> .git/info/sparse-checkout
else
    echo "Modo DESARROLLO seleccionado. Descarga completa..."
    git config core.sparseCheckout false
fi

# 5. Descargar contenido
echo "Sincronizando con rama '$BRANCH'..."
git pull origin "$BRANCH"
if [ "$MODE" != "1" ]; then git checkout "$BRANCH"; fi

# Limpieza extra para Modo Servidor
if [ "$MODE" == "1" ]; then
    echo "Limpiando archivos residuales..."
    git clean -fdX
fi

echo "--- Operación Completada ---"