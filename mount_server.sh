#!/bin/bash

# Script de instalación de dependencias para LMMfunction (Setup Completo - Ubuntu/Debian)
# Automatiza la instalación de Java 21, Maven, Cloudflared y utilidades base.

echo "--- Iniciando configuración del Servidor Persepolis ---"

ROOT_DIR="LMMfunction"

# Función para verificar si un comando existe
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Verificación de permisos (sudo)
if [ "$EUID" -ne 0 ]; then
    echo "Este script requiere permisos de superusuario para instalar paquetes del sistema."
    echo "Por favor, ejecútelo con sudo: sudo ./mount_server.sh"
    exit 1
fi

CONFIG_FILE="server.conf"

echo "--- 1. Actualizando sistema y paquetes base ---"
apt-get update -qq

# Lista de paquetes base y utilidades (excluyendo git según solicitud)
BASE_PACKAGES="ca-certificates curl wget gnupg lsb-release software-properties-common tmux openssh-server"

echo "Instalando utilidades base: $BASE_PACKAGES..."
apt-get install -y $BASE_PACKAGES

echo "--- 2. Instalando Java 21 (Crítico para Spring Boot 4.0) ---"
if ! command_exists java || ! java -version 2>&1 | grep -q "21"; then
    echo "Java 21 no detectado o versión incorrecta. Instalando OpenJDK 21..."
    apt-get install -y openjdk-21-jdk
else
    echo "Java 21 ya está instalado."
fi

echo "--- 3. Instalando Maven ---"
if ! command_exists mvn; then
    echo "Maven no detectado. Instalando..."
    apt-get install -y maven
else
    echo "Maven ya está instalado."
fi

echo "--- 4. Instalando Cloudflare Tunnel (cloudflared) ---"
if ! command_exists cloudflared; then
    echo "Cloudflared no detectado. Configurando repositorio e instalando..."
    # Añadir clave GPG de Cloudflare
    mkdir -p --mode=0755 /usr/share/keyrings
    curl -fsSL https://pkg.cloudflare.com/cloudflare-main.gpg | tee /usr/share/keyrings/cloudflare-main.gpg >/dev/null
    
    # Añadir repositorio (asumiendo jammy/22.04 o compatible)
    echo 'deb [signed-by=/usr/share/keyrings/cloudflare-main.gpg] https://pkg.cloudflare.com/cloudflared jammy main' | tee /etc/apt/sources.list.d/cloudflared.list
    
    apt-get update -qq
    apt-get install -y cloudflared
else
    echo "Cloudflared ya está instalado."
fi

echo "--- 5. Configuración de dependencias del proyecto ($ROOT_DIR) ---"

# Cambiar propietario de la carpeta al usuario real si se ejecuta con sudo
if [ -n "$SUDO_USER" ]; then
    REAL_USER=$SUDO_USER
    echo "Ajustando permisos para el usuario: $REAL_USER"
    chown -R $REAL_USER:$REAL_USER "$ROOT_DIR" 2>/dev/null
fi

if [ ! -d "$ROOT_DIR" ]; then
    echo "Advertencia: No se encuentra la carpeta '$ROOT_DIR'. Saltando compilación del proyecto."
else
    # Buscar todos los directorios dentro de LMMfunction (incluyendo la raíz)
    find "$ROOT_DIR" -type d | while read dir; do
        (
            cd "$dir" || exit
            
            # --- Java (Maven) ---
            if [ -f "pom.xml" ]; then
                echo "[$dir] Detectado pom.xml. Ejecutando Maven install..."
                # Ejecutar maven como el usuario real si es posible, sino como root
                if [ -n "$SUDO_USER" ]; then
                    sudo -u $SUDO_USER mvn clean install -DskipTests
                else
                    mvn clean install -DskipTests
                fi
            fi
        )
    done
fi

echo "--- 6. Configurando Script de Control ---"
CONTROL_SCRIPT="control_server.sh"

if [ -f "$CONTROL_SCRIPT" ]; then
    chmod +x "$CONTROL_SCRIPT"
    # Crear enlace simbólico global para poder usar 'control' desde cualquier lugar
    ln -sf "$(pwd)/$CONTROL_SCRIPT" /usr/local/bin/control
    echo "✅ Comando 'control' configurado globalmente."
else
    echo "Advertencia: $CONTROL_SCRIPT no encontrado en el directorio actual."
fi

echo "--- 8. Configurando Archivo de Configuración ($CONFIG_FILE) ---"

# Función para leer o preguntar por una variable
get_config_value() {
    VAR_NAME=$1
    VAR_PROMPT=$2
    
    # Intentar leer desde el archivo, sino preguntar
    if [ -f "$CONFIG_FILE" ]; then
        VALUE=$(grep "^${VAR_NAME}=" "$CONFIG_FILE" | cut -d'=' -f2)
    fi
    
    if [ -z "$VALUE" ]; then
        read -p "$VAR_PROMPT: " VALUE
        echo "${VAR_NAME}=${VALUE}" >> "$CONFIG_FILE"
    fi
    
    echo "$VALUE"
}

# Preguntar por las variables si no existen
DB_USER=$(get_config_value "DB_USER" "Usuario de la base de datos (SQL)")
DB_PASSWORD=$(get_config_value "DB_PASSWORD" "Contraseña de la base de datos (SQL)")
TELEGRAM_BOT_TOKEN=$(get_config_value "TELEGRAM_BOT_TOKEN" "Token del bot de Telegram")
TELEGRAM_CHAT_ID=$(get_config_value "TELEGRAM_CHAT_ID" "ID del chat de Telegram")
OPENROUTER_KEY=$(get_config_value "OPENROUTER_KEY" "OpenRouter API Key")
OPENROUTER_MODEL=$(get_config_value "OPENROUTER_MODEL" "Modelo de OpenRouter")

# Menú para editar la configuración
edit_config() {
    while true; do
        echo -e "\n--- Editar Configuración ---"
        echo "1. Usuario de la base de datos"
        echo "2. Contraseña de la base de datos"
        echo "3. Token del bot de Telegram"
        echo "4. ID del chat de Telegram"
        echo "5. OpenRouter API Key"
        echo "6. Modelo de OpenRouter"
        echo "7. Mostrar configuración actual"
        echo "8. Salir"
        read -p "Seleccione una opción: " choice

        case "$choice" in
            1) read -p "Nuevo usuario de la base de datos: " NEW_VALUE; sed -i "s/^DB_USER=.*/DB_USER=$NEW_VALUE/" "$CONFIG_FILE";;
            2) read -p "Nueva contraseña de la base de datos: " NEW_VALUE; sed -i "s/^DB_PASSWORD=.*/DB_PASSWORD=$NEW_VALUE/" "$CONFIG_FILE";;
            3) read -p "Nuevo token del bot de Telegram: " NEW_VALUE; sed -i "s/^TELEGRAM_BOT_TOKEN=.*/TELEGRAM_BOT_TOKEN=$NEW_VALUE/" "$CONFIG_FILE";;
            4) read -p "Nuevo ID del chat de Telegram: " NEW_VALUE; sed -i "s/^TELEGRAM_CHAT_ID=.*/TELEGRAM_CHAT_ID=$NEW_VALUE/" "$CONFIG_FILE";;
            5) read -p "Nueva OpenRouter API Key: " NEW_VALUE; sed -i "s/^OPENROUTER_KEY=.*/OPENROUTER_KEY=$NEW_VALUE/" "$CONFIG_FILE";;
            6) read -p "Nuevo Modelo de OpenRouter: " NEW_VALUE; sed -i "s/^OPENROUTER_MODEL=.*/OPENROUTER_MODEL=$NEW_VALUE/" "$CONFIG_FILE";;
            7) cat "$CONFIG_FILE";;
            8) break;;
            *) echo "Opción no válida.";;
        esac
    done
}
echo "--- 7. Configurando Accesos Directos (server-start / server-mount) ---"

# Configurar start_server.sh
if [ -f "start_server.sh" ]; then
    chmod +x "start_server.sh"
    ln -sf "$(pwd)/start_server.sh" /usr/local/bin/server-start
    echo "✅ Comando 'server-start' configurado."
fi

# Configurar mount_server.sh (este script)
# Usamos 'server-mount' porque 'mount' es un comando del sistema.
chmod +x "mount_server.sh"
ln -sf "$(pwd)/mount_server.sh" /usr/local/bin/server-mount
echo "✅ Comando 'server-mount' configurado."

echo "--- Setup Finalizado ---"

# Preguntar si desea iniciar el servidor
read -p "¿Desea iniciar el servidor y el túnel ahora? (S/N): " START_NOW
if [[ "$START_NOW" == "S" || "$START_NOW" == "s" ]]; then
    # Ejecutar como el usuario real (no root) si es posible, para que el tmux sea del usuario
    if [ -n "$SUDO_USER" ]; then
        sudo -u $SUDO_USER control start
    else
        control start
    fi
    
    edit_config
else
    echo "Comandos disponibles: 'control start', 'server-start', 'server-mount'"
fi