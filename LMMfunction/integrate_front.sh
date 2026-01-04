#!/bin/bash

# Configuración de rutas (Relativas a donde está este script: LMMfunction)
SOURCE_DIR="../front/Front"
DEST_DIR="src/main/resources/static"

echo "=========================================="
echo "   INTEGRANDO FRONTEND DEL DISEÑADOR      "
echo "=========================================="

# 1. Comprobar que existe la carpeta del diseñador
if [ ! -d "$SOURCE_DIR" ]; then
    echo "ERROR: No encuentro la carpeta del diseñador en: $SOURCE_DIR"
    exit 1
fi

# 2. Compilar el CSS (Tailwind) en la carpeta del diseñador
echo "--> Compilando Tailwind CSS..."
cd "$SOURCE_DIR" || exit
if [ ! -d "node_modules" ]; then
    echo "    (Instalando dependencias npm...)"
    npm install
fi
npm run build
if [ $? -ne 0 ]; then
    echo "ERROR: Falló la compilación de Tailwind."
    exit 1
fi
cd - > /dev/null || exit

# 3. Copiar los archivos
echo "--> Limpiando y copiando archivos a static (filtrando)..."
mkdir -p "$DEST_DIR"
rm -rf "${DEST_DIR:?}"/*

# Copiar excluyendo node_modules y archivos de configuración
if command -v rsync &> /dev/null; then
    rsync -av --exclude='node_modules' --exclude='.git' --exclude='package.json' --exclude='package-lock.json' --exclude='tailwind.config.js' --exclude='.gitignore' "$SOURCE_DIR/" "$DEST_DIR/"
else
    for file in "$SOURCE_DIR"/*; do
        name=$(basename "$file")
        case "$name" in
            node_modules|.git|package.json|package-lock.json|tailwind.config.js|.gitignore) ;;
            *) cp -r "$file" "$DEST_DIR/" ;;
        esac
    done
fi

echo "INTEGRACIÓN COMPLETADA. Ahora puedes ejecutar 'mvn clean package'."
