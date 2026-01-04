#!/bin/bash

echo "=========================================="
echo "   SINCRONIZACIÓN CON GITHUB (EASY MODE)  "
echo "=========================================="

# 0. AUTO-CONFIGURACIÓN (Solo la primera vez)
# Comprobamos si Git sabe quién eres. Si no, lo configuramos.
if [ -z "$(git config user.name)" ]; then
    echo "👋 ¡Hola! Parece que es tu primera vez ejecutando esto."
    echo "Necesito saber quién eres para firmar tus cambios."
    
    read -p "--> Escribe tu Nombre: " git_name
    read -p "--> Escribe tu Email: " git_email
    
    git config --global user.name "$git_name"
    git config --global user.email "$git_email"
    
    # Configurar para que recuerde la contraseña/token y no la pida siempre
    if [ -z "$(git config credential.helper)" ]; then
        git config --global credential.helper store
    fi
    echo "✅ Configuración guardada. ¡Listo!"
fi

# 1. Verificar repositorio
if [ ! -d ".git" ]; then
    echo "❌ ERROR: No estás en la raíz del repositorio o no hay git iniciado."
    exit 1
fi

# 2. Traer cambios de la nube (Pull)
echo "--> 1. Descargando cambios de tus compañeros (git pull)..."
git pull
if [ $? -ne 0 ]; then
    echo "❌ ERROR: Hay conflictos al descargar. Debes resolverlos manualmente."
    exit 1
fi

# 3. Mostrar estado y confirmar
echo "------------------------------------------"
echo "Archivos modificados localmente:"
git status -s
echo "------------------------------------------"

read -p "¿Quieres subir estos cambios a la nube? (s/n): " confirm
if [[ "$confirm" != "s" && "$confirm" != "S" ]]; then
    echo "Operación cancelada. Solo se descargaron los cambios remotos."
    exit 0
fi

# 4. Proceso de subida
echo "--> 2. Añadiendo archivos..."
git add .

read -p "--> Escribe un mensaje para el commit (qué hiciste): " msg
if [ -z "$msg" ]; then
    msg="Actualización automática del equipo"
fi

git commit -m "$msg"
echo "--> 3. Subiendo a GitHub..."
git push
echo "   (Si te pide contraseña, recuerda usar tu Token de GitHub)"

echo "✅ ¡LISTO! Todo está sincronizado."