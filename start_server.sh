#!/bin/bash
# Script de gestión para Servidor Persepolis (Linux/Mac)

REPO_URL="https://github.com/marquistallman/Persepolis.git"

echo "--- Gestor de Servidor Persepolis ---"

# Asegurar git
if [ ! -d ".git" ]; then
    echo "Inicializando repositorio..."
    git init
    git remote add origin "$REPO_URL"
fi

echo -e "\nMenú de Opciones:"
echo "1. Recibir cambios de 'developer' (Actualizar)"
echo "2. Instalar Sistema (LMMfunction y data)"
echo "3. Salir"
read -p "Seleccione una opción: " OPTION

if [ "$OPTION" == "1" ]; then
    echo "Conectando con rama 'developer'..."
    git fetch origin developer
    if [ $? -ne 0 ]; then echo "Error: No se pudo conectar con el repositorio."; exit 1; fi
    
    echo "Recibiendo cambios..."
    git checkout developer
    git pull origin developer

elif [ "$OPTION" == "2" ]; then
    echo "Configurando instalación del sistema..."
    
    # Configurar Sparse Checkout
    git config core.sparseCheckout true
    mkdir -p .git/info
    
    echo "LMMfunction/" > .git/info/sparse-checkout
    echo "data/" >> .git/info/sparse-checkout
    echo "setup_server.ps1" >> .git/info/sparse-checkout
    echo "setup_server.sh" >> .git/info/sparse-checkout
    echo "README.md" >> .git/info/sparse-checkout
    
    echo "Descargando archivos del sistema..."
    git fetch origin developer
    if [ $? -ne 0 ]; then echo "Error: No se pudo conectar con el repositorio."; exit 1; fi
    
    # Forzar estado
    git checkout -f -B developer origin/developer
    
    echo "Limpiando archivos no necesarios..."
    git clean -fdx

elif [ "$OPTION" == "3" ]; then
    exit 0
else
    echo "Opción no válida."
fi

echo "--- Proceso Finalizado ---"