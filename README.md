# Guía de Despliegue y Desarrollo - Persepolis Server

Este repositorio contiene la configuración automatizada para desplegar el servidor Persepolis de manera eficiente.

## Scripts de Instalación

El proyecto incluye scripts interactivos que gestionan la configuración de Git y las descargas de archivos según el entorno.

- **Windows:** `setup_server.ps1`
- **Linux/Mac:** `setup_server.sh`

## Modos de Uso

Al ejecutar los scripts, podrás seleccionar entre dos modos:

### 1. Modo Servidor (Producción)
Optimizado para VPS y entornos de despliegue.
- **Tecnología:** Utiliza *Git Sparse Checkout*.
- **Comportamiento:** Solo descarga las carpetas esenciales (`LMMfunction`, `data`) y los scripts de mantenimiento.
- **Ventaja:** Ahorra espacio en disco y ancho de banda. Mantiene el directorio limpio.

### 2. Modo Desarrollo (Local)
Para programadores y colaboradores.
- **Comportamiento:** Descarga el repositorio completo (todas las ramas y carpetas).
- **Ventaja:** Acceso total al código para realizar cambios y `commits`.

## Instrucciones Rápidas

### En Windows
1. Abre PowerShell en la carpeta del proyecto.
2. Ejecuta:
   ```powershell
   .\setup_server.ps1
   ```
3. Sigue las instrucciones en pantalla.

### En Linux (VPS)
1. Sube el archivo `setup_server.sh` al servidor.
2. Dale permisos de ejecución y ejecútalo:
   ```bash
   chmod +x setup_server.sh
   ./setup_server.sh
   ```
