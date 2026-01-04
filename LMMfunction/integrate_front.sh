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
echo "--> Limpiando y copiando archivos a static..."
mkdir -p "$DEST_DIR"
rm -rf "${DEST_DIR:?}"/*
cp -r "$SOURCE_DIR"/* "$DEST_DIR"/

# 4. Limpieza de archivos de desarrollo en destino
echo "--> Eliminando archivos innecesarios (node_modules, configs)..."
rm -rf "$DEST_DIR/node_modules" "$DEST_DIR/.git" "$DEST_DIR/package.json" "$DEST_DIR/package-lock.json" "$DEST_DIR/tailwind.config.js" "$DEST_DIR/.gitignore"

echo "INTEGRACIÓN COMPLETADA. Ahora puedes ejecutar 'mvn clean package'."
