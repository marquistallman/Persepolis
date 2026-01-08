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
    # Copiar carpetas especificas sin cambiar de rama
    git checkout origin/developer -- LMMfunction/ data/

elif [ "$OPTION" == "2" ]; then
    echo "Configurando instalación del sistema..."
    
    # Configurar Sparse Checkout
    git config core.sparseCheckout true
    mkdir -p .git/info
    
    echo "LMMfunction/" > .git/info/sparse-checkout
    echo "data/" >> .git/info/sparse-checkout
    echo "start_server.ps1" >> .git/info/sparse-checkout
    echo "start_server.sh" >> .git/info/sparse-checkout
    echo "mount_server.ps1" >> .git/info/sparse-checkout
    echo "mount_server.sh" >> .git/info/sparse-checkout
    echo "README.md" >> .git/info/sparse-checkout
    
    echo "Descargando archivos del sistema..."
    git fetch origin developer
    if [ $? -ne 0 ]; then echo "Error: No se pudo conectar con el repositorio."; exit 1; fi
    
    # Copiar carpetas especificas sin cambiar de rama
    git checkout origin/developer -- LMMfunction/ data/

elif [ "$OPTION" == "3" ]; then
    exit 0
else
    echo "Opción no válida."
fi

if [ "$OPTION" == "1" ] || [ "$OPTION" == "2" ]; then
    echo ""
    read -p "¿Desea continuar ejecutando 'mount_server.sh'? (S/N): " CONTINUE
    if [[ "$CONTINUE" == "S" || "$CONTINUE" == "s" ]]; then
        if [ -f "mount_server.sh" ]; then
            chmod +x mount_server.sh
            ./mount_server.sh
        else
            echo "Error: mount_server.sh no encontrado."
        fi
    fi
fi

echo "--- Proceso Finalizado ---"